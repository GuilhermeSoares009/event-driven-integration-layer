package com.eventdriven.integrationlayer.outbox;

import com.eventdriven.integrationlayer.processing.RetryBackoffPolicy;
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

@Component
public class OutboxPublishingWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublishingWorker.class);

    private final OutboxMessageRepository outboxMessageRepository;
    private final OutboxPublisherRouter publisherRouter;
    private final RetryBackoffPolicy retryBackoffPolicy;
    private final int batchSize;
    private final int maxAttempts;

    public OutboxPublishingWorker(
        OutboxMessageRepository outboxMessageRepository,
        OutboxPublisherRouter publisherRouter,
        RetryBackoffPolicy retryBackoffPolicy,
        @Value("${integration.processing.outbox.batch-size:50}") int batchSize,
        @Value("${integration.processing.outbox.max-attempts:5}") int maxAttempts
    ) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.publisherRouter = publisherRouter;
        this.retryBackoffPolicy = retryBackoffPolicy;
        this.batchSize = Math.max(1, batchSize);
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Scheduled(fixedDelayString = "${integration.processing.outbox.schedule-ms:2000}")
    public void processBatch() {
        Pageable page = PageRequest.of(0, batchSize);

        List<OutboxMessage> pending = outboxMessageRepository.findByStatusOrderByCreatedAtAsc(
            OutboxMessage.STATUS_PENDING,
            page
        );
        processCandidates(pending, OutboxMessage.STATUS_PENDING);

        List<OutboxMessage> failedDue = outboxMessageRepository.findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            OutboxMessage.STATUS_FAILED,
            OffsetDateTime.now(ZoneOffset.UTC),
            page
        );
        processCandidates(failedDue, OutboxMessage.STATUS_FAILED);
    }

    private void processCandidates(List<OutboxMessage> messages, String expectedStatus) {
        for (OutboxMessage message : messages) {
            processSingle(message.getId(), expectedStatus);
        }
    }

    private void processSingle(Long outboxMessageId, String expectedStatus) {
        int claimed = outboxMessageRepository.transitionStatus(
            outboxMessageId,
            expectedStatus,
            OutboxMessage.STATUS_SENDING
        );
        if (claimed == 0) {
            return;
        }

        OutboxMessage message = outboxMessageRepository.findById(outboxMessageId).orElse(null);
        if (message == null) {
            return;
        }

        try {
            publisherRouter.resolve(message).publish(message);

            message.setStatus(OutboxMessage.STATUS_SENT);
            message.setSentAt(OffsetDateTime.now(ZoneOffset.UTC));
            message.setNextRetryAt(null);
            outboxMessageRepository.save(message);

            log.info(
                "outbox.sent outbox_message_id={} type={} correlation_id={}",
                message.getId(),
                message.getType(),
                message.getCorrelationId()
            );
        } catch (Exception exception) {
            handleFailure(message, exception);
        }
    }

    private void handleFailure(OutboxMessage message, Exception exception) {
        int attempts = (message.getAttempts() == null ? 0 : message.getAttempts()) + 1;
        boolean dead = attempts >= maxAttempts;

        message.setAttempts(attempts);
        message.setLastError(exception.getMessage());

        if (dead) {
            message.setStatus(OutboxMessage.STATUS_DEAD);
            message.setNextRetryAt(null);
            log.warn(
                "outbox.dead outbox_message_id={} type={} correlation_id={}",
                message.getId(),
                message.getType(),
                message.getCorrelationId()
            );
        } else {
            message.setStatus(OutboxMessage.STATUS_FAILED);
            message.setNextRetryAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(retryBackoffPolicy.nextDelaySeconds(attempts)));
            log.warn(
                "outbox.failed outbox_message_id={} type={} correlation_id={} error={}",
                message.getId(),
                message.getType(),
                message.getCorrelationId(),
                exception.getMessage()
            );
        }

        outboxMessageRepository.save(message);
    }
}
