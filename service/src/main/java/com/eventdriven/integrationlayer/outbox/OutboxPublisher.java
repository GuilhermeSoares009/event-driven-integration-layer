package com.eventdriven.integrationlayer.outbox;

public interface OutboxPublisher {

    void publish(OutboxMessage message) throws Exception;
}
