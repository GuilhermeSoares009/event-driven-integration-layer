package com.eventdriven.integrationlayer.ingestion;

public enum WebhookIngestionOutcome {
    ACCEPTED,
    DEDUPED,
    INVALID_SIGNATURE,
    INVALID_PAYLOAD
}
