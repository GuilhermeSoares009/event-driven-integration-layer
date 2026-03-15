package com.eventdriven.integrationlayer.ingestion;

public interface WebhookSignatureValidator {

    boolean isValid(String provider, String payload, String signature);
}
