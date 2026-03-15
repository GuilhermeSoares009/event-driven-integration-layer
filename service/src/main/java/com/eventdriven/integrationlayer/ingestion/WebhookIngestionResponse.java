package com.eventdriven.integrationlayer.ingestion;

public record WebhookIngestionResponse(
    WebhookIngestionOutcome outcome,
    String correlationId,
    Long inboxEventId
) {

    public static WebhookIngestionResponse circuitOpen() {
        return new WebhookIngestionResponse(WebhookIngestionOutcome.CIRCUIT_OPEN, null, null);
    }

    public static WebhookIngestionResponse accepted(String correlationId, Long inboxEventId) {
        return new WebhookIngestionResponse(WebhookIngestionOutcome.ACCEPTED, correlationId, inboxEventId);
    }

    public static WebhookIngestionResponse deduped(String correlationId) {
        return new WebhookIngestionResponse(WebhookIngestionOutcome.DEDUPED, correlationId, null);
    }

    public static WebhookIngestionResponse invalidSignature() {
        return new WebhookIngestionResponse(WebhookIngestionOutcome.INVALID_SIGNATURE, null, null);
    }

    public static WebhookIngestionResponse invalidPayload() {
        return new WebhookIngestionResponse(WebhookIngestionOutcome.INVALID_PAYLOAD, null, null);
    }
}
