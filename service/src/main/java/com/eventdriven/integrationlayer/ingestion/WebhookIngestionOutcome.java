package com.eventdriven.integrationlayer.ingestion;

public enum WebhookIngestionOutcome {
    CIRCUIT_OPEN,
    ACCEPTED,
    DEDUPED,
    INVALID_SIGNATURE,
    INVALID_PAYLOAD
}
