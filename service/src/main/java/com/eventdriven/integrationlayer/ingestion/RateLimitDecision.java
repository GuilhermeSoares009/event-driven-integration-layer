package com.eventdriven.integrationlayer.ingestion;

public record RateLimitDecision(boolean limited, int retryAfterSeconds) {

    public static RateLimitDecision allowed() {
        return new RateLimitDecision(false, 0);
    }

    public static RateLimitDecision limited(int retryAfterSeconds) {
        return new RateLimitDecision(true, Math.max(1, retryAfterSeconds));
    }
}
