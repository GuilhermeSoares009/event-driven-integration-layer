package com.eventdriven.integrationlayer.outbox;

import org.springframework.stereotype.Component;

@Component
public class OutboxPublisherRouter {

    private final KafkaOutboxPublisher kafkaOutboxPublisher;

    public OutboxPublisherRouter(KafkaOutboxPublisher kafkaOutboxPublisher) {
        this.kafkaOutboxPublisher = kafkaOutboxPublisher;
    }

    public OutboxPublisher resolve(OutboxMessage message) {
        return kafkaOutboxPublisher;
    }
}
