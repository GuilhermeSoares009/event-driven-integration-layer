package com.eventdriven.integrationlayer.outbox;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaOutboxPublisher implements OutboxPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topicPrefix;

    public KafkaOutboxPublisher(
        KafkaTemplate<String, String> kafkaTemplate,
        @Value("${integration.outbox.kafka.topic-prefix:integration.outbox}") String topicPrefix
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicPrefix = topicPrefix;
    }

    @Override
    public void publish(OutboxMessage message) throws Exception {
        String topic = topicPrefix + "." + message.getType();
        kafkaTemplate
            .send(topic, message.getCorrelationId(), message.getPayloadJson())
            .get(10, TimeUnit.SECONDS);
    }
}
