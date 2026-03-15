package com.eventdriven.integrationlayer.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventdriven.integrationlayer.inbox.InboxEvent;
import com.eventdriven.integrationlayer.inbox.InboxEventRepository;
import com.eventdriven.integrationlayer.outbox.OutboxMessage;
import com.eventdriven.integrationlayer.outbox.OutboxMessageRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class InboxProcessingWorkerTest {

    @Mock
    private InboxEventRepository inboxEventRepository;

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    @Mock
    private InboxHandlerRouter inboxHandlerRouter;

    @Mock
    private RetryBackoffPolicy retryBackoffPolicy;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private InboxEventHandler inboxEventHandler;

    private InboxProcessingWorker worker;

    @BeforeEach
    void setUp() {
        worker = new InboxProcessingWorker(
            inboxEventRepository,
            outboxMessageRepository,
            inboxHandlerRouter,
            retryBackoffPolicy,
            transactionTemplate,
            10,
            5
        );

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        when(inboxEventRepository.findByStatusAndNextRetryAtLessThanEqualOrderByReceivedAtAsc(
            eq(InboxEvent.STATUS_FAILED),
            any(OffsetDateTime.class),
            any(Pageable.class)
        )).thenReturn(List.of());
    }

    @Test
    void shouldProcessReceivedEventAndCreateOutboxMessage() {
        InboxEvent event = buildEvent(1L, InboxEvent.STATUS_RECEIVED, 0);

        when(inboxEventRepository.findByStatusOrderByReceivedAtAsc(eq(InboxEvent.STATUS_RECEIVED), any(Pageable.class)))
            .thenReturn(List.of(event));
        when(inboxEventRepository.transitionStatus(1L, InboxEvent.STATUS_RECEIVED, InboxEvent.STATUS_PROCESSING))
            .thenReturn(1);
        when(inboxEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(inboxHandlerRouter.resolve(event)).thenReturn(inboxEventHandler);

        worker.processBatch();

        ArgumentCaptor<OutboxMessage> outboxCaptor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMessageRepository).save(outboxCaptor.capture());

        OutboxMessage outbox = outboxCaptor.getValue();
        assertEquals(OutboxMessage.STATUS_PENDING, outbox.getStatus());
        assertEquals("orders", outbox.getType());
        assertEquals(event.getPayloadJson(), outbox.getPayloadJson());
        assertEquals("corr-1", outbox.getCorrelationId());

        ArgumentCaptor<InboxEvent> inboxCaptor = ArgumentCaptor.forClass(InboxEvent.class);
        verify(inboxEventRepository).save(inboxCaptor.capture());

        InboxEvent processed = inboxCaptor.getValue();
        assertEquals(InboxEvent.STATUS_PROCESSED, processed.getStatus());
        assertNotNull(processed.getProcessedAt());
    }

    @Test
    void shouldMarkEventAsFailedWhenHandlerThrows() {
        InboxEvent event = buildEvent(2L, InboxEvent.STATUS_RECEIVED, 0);

        when(inboxEventRepository.findByStatusOrderByReceivedAtAsc(eq(InboxEvent.STATUS_RECEIVED), any(Pageable.class)))
            .thenReturn(List.of(event));
        when(inboxEventRepository.transitionStatus(2L, InboxEvent.STATUS_RECEIVED, InboxEvent.STATUS_PROCESSING))
            .thenReturn(1);
        when(inboxEventRepository.findById(2L)).thenReturn(Optional.of(event));
        when(inboxHandlerRouter.resolve(event)).thenReturn(inboxEventHandler);
        when(retryBackoffPolicy.nextDelaySeconds(1)).thenReturn(30);
        doThrow(new RuntimeException("boom")).when(inboxEventHandler).handle(event);

        worker.processBatch();

        verify(outboxMessageRepository, never()).save(any(OutboxMessage.class));

        ArgumentCaptor<InboxEvent> inboxCaptor = ArgumentCaptor.forClass(InboxEvent.class);
        verify(inboxEventRepository).save(inboxCaptor.capture());

        InboxEvent failed = inboxCaptor.getValue();
        assertEquals(InboxEvent.STATUS_FAILED, failed.getStatus());
        assertEquals(1, failed.getAttempts());
        assertNotNull(failed.getNextRetryAt());
    }

    private InboxEvent buildEvent(Long id, String status, int attempts) {
        InboxEvent event = new InboxEvent();
        event.setId(id);
        event.setProvider("test");
        event.setTopic("orders");
        event.setStatus(status);
        event.setAttempts(attempts);
        event.setPayloadJson("{\"foo\":\"bar\"}");
        event.setCorrelationId("corr-1");
        event.setReceivedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return event;
    }
}
