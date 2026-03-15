package com.eventdriven.integrationlayer.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventdriven.integrationlayer.processing.RetryBackoffPolicy;
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

@ExtendWith(MockitoExtension.class)
class OutboxPublishingWorkerTest {

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    @Mock
    private OutboxPublisherRouter outboxPublisherRouter;

    @Mock
    private RetryBackoffPolicy retryBackoffPolicy;

    @Mock
    private OutboxPublisher outboxPublisher;

    private OutboxPublishingWorker worker;

    @BeforeEach
    void setUp() {
        worker = new OutboxPublishingWorker(
            outboxMessageRepository,
            outboxPublisherRouter,
            retryBackoffPolicy,
            10,
            5
        );

        when(outboxMessageRepository.findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxMessage.STATUS_FAILED),
            any(OffsetDateTime.class),
            any(Pageable.class)
        )).thenReturn(List.of());
    }

    @Test
    void shouldMarkOutboxMessageAsSentWhenPublishSucceeds() throws Exception {
        OutboxMessage message = buildMessage(10L, OutboxMessage.STATUS_PENDING, 0);

        when(outboxMessageRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxMessage.STATUS_PENDING), any(Pageable.class)))
            .thenReturn(List.of(message));
        when(outboxMessageRepository.transitionStatus(10L, OutboxMessage.STATUS_PENDING, OutboxMessage.STATUS_SENDING))
            .thenReturn(1);
        when(outboxMessageRepository.findById(10L)).thenReturn(Optional.of(message));
        when(outboxPublisherRouter.resolve(message)).thenReturn(outboxPublisher);

        worker.processBatch();

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMessageRepository).save(captor.capture());

        OutboxMessage sent = captor.getValue();
        assertEquals(OutboxMessage.STATUS_SENT, sent.getStatus());
        assertNotNull(sent.getSentAt());
    }

    @Test
    void shouldMarkOutboxMessageAsFailedWhenPublishThrows() throws Exception {
        OutboxMessage message = buildMessage(11L, OutboxMessage.STATUS_PENDING, 0);

        when(outboxMessageRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxMessage.STATUS_PENDING), any(Pageable.class)))
            .thenReturn(List.of(message));
        when(outboxMessageRepository.transitionStatus(11L, OutboxMessage.STATUS_PENDING, OutboxMessage.STATUS_SENDING))
            .thenReturn(1);
        when(outboxMessageRepository.findById(11L)).thenReturn(Optional.of(message));
        when(outboxPublisherRouter.resolve(message)).thenReturn(outboxPublisher);
        when(retryBackoffPolicy.nextDelaySeconds(1)).thenReturn(30);
        doThrow(new RuntimeException("publish error")).when(outboxPublisher).publish(message);

        worker.processBatch();

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMessageRepository).save(captor.capture());

        OutboxMessage failed = captor.getValue();
        assertEquals(OutboxMessage.STATUS_FAILED, failed.getStatus());
        assertEquals(1, failed.getAttempts());
        assertNotNull(failed.getNextRetryAt());
    }

    private OutboxMessage buildMessage(Long id, String status, int attempts) {
        OutboxMessage message = new OutboxMessage();
        message.setId(id);
        message.setType("orders");
        message.setStatus(status);
        message.setAttempts(attempts);
        message.setPayloadJson("{\"foo\":\"bar\"}");
        message.setCorrelationId("corr-1");
        message.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return message;
    }
}
