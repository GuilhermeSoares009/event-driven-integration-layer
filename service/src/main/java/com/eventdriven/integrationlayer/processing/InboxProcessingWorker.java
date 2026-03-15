package com.eventdriven.integrationlayer.processing;

import com.eventdriven.integrationlayer.inbox.InboxEvent;
import com.eventdriven.integrationlayer.inbox.InboxEventRepository;
import com.eventdriven.integrationlayer.outbox.OutboxMessage;
import com.eventdriven.integrationlayer.outbox.OutboxMessageRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class InboxProcessingWorker {

    private static final Logger log = LoggerFactory.getLogger(InboxProcessingWorker.class);

    private final InboxEventRepository inboxEventRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final InboxHandlerRouter handlerRouter;
    private final RetryBackoffPolicy retryBackoffPolicy;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;
    private final int maxAttempts;

    public InboxProcessingWorker(
        InboxEventRepository inboxEventRepository,
        OutboxMessageRepository outboxMessageRepository,
        InboxHandlerRouter handlerRouter,
        RetryBackoffPolicy retryBackoffPolicy,
        TransactionTemplate transactionTemplate,
        @Value("${integration.processing.inbox.batch-size:50}") int batchSize,
        @Value("${integration.processing.inbox.max-attempts:5}") int maxAttempts
    ) {
        this.inboxEventRepository = inboxEventRepository;
        this.outboxMessageRepository = outboxMessageRepository;
        this.handlerRouter = handlerRouter;
        this.retryBackoffPolicy = retryBackoffPolicy;
        this.transactionTemplate = transactionTemplate;
        this.batchSize = Math.max(1, batchSize);
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Scheduled(fixedDelayString = "${integration.processing.inbox.schedule-ms:2000}")
    public void processBatch() {
        Pageable page = PageRequest.of(0, batchSize);

        List<InboxEvent> received = inboxEventRepository.findByStatusOrderByReceivedAtAsc(
            InboxEvent.STATUS_RECEIVED,
            page
        );
        processCandidates(received, InboxEvent.STATUS_RECEIVED);

        List<InboxEvent> failedDue = inboxEventRepository.findByStatusAndNextRetryAtLessThanEqualOrderByReceivedAtAsc(
            InboxEvent.STATUS_FAILED,
            OffsetDateTime.now(ZoneOffset.UTC),
            page
        );
        processCandidates(failedDue, InboxEvent.STATUS_FAILED);
    }

    private void processCandidates(List<InboxEvent> events, String expectedStatus) {
        for (InboxEvent event : events) {
            processSingle(event.getId(), expectedStatus);
        }
    }

    private void processSingle(Long eventId, String expectedStatus) {
        int claimed = inboxEventRepository.transitionStatus(
            eventId,
            expectedStatus,
            InboxEvent.STATUS_PROCESSING
        );
        if (claimed == 0) {
            return;
        }

        InboxEvent event = inboxEventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return;
        }

        try {
            processClaimedEvent(event);
        } catch (Throwable exception) {
            handleFailure(event, exception);
        }
    }

    private void processClaimedEvent(InboxEvent event) {
        transactionTemplate.executeWithoutResult(status -> {
            handlerRouter.resolve(event).handle(event);

            OutboxMessage outboxMessage = new OutboxMessage();
            outboxMessage.setType(event.getTopic());
            outboxMessage.setPayloadJson(event.getPayloadJson());
            outboxMessage.setStatus(OutboxMessage.STATUS_PENDING);
            outboxMessage.setAttempts(0);
            outboxMessage.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            outboxMessage.setCorrelationId(event.getCorrelationId());
            outboxMessageRepository.save(outboxMessage);

            event.setStatus(InboxEvent.STATUS_PROCESSED);
            event.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
            event.setNextRetryAt(null);
            inboxEventRepository.save(event);

            log.info(
                "inbox.processed inbox_event_id={} provider={} topic={} correlation_id={}",
                event.getId(),
                event.getProvider(),
                event.getTopic(),
                event.getCorrelationId()
            );
        });
    }

    private void handleFailure(InboxEvent event, Throwable exception) {
        int attempts = (event.getAttempts() == null ? 0 : event.getAttempts()) + 1;
        boolean dead = attempts >= maxAttempts;

        event.setAttempts(attempts);
        event.setLastErrorCode(exception.getClass().getName());
        event.setLastErrorMessage(exception.getMessage());

        if (dead) {
            event.setStatus(InboxEvent.STATUS_DEAD);
            event.setNextRetryAt(null);
            log.warn(
                "inbox.dead inbox_event_id={} provider={} topic={} correlation_id={}",
                event.getId(),
                event.getProvider(),
                event.getTopic(),
                event.getCorrelationId()
            );
        } else {
            event.setStatus(InboxEvent.STATUS_FAILED);
            event.setNextRetryAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(retryBackoffPolicy.nextDelaySeconds(attempts)));
            log.warn(
                "inbox.failed inbox_event_id={} provider={} topic={} correlation_id={} error={}",
                event.getId(),
                event.getProvider(),
                event.getTopic(),
                event.getCorrelationId(),
                exception.getMessage()
            );
        }

        inboxEventRepository.save(event);
    }
}
