package com.eventdriven.integrationlayer.processing;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class RetryBackoffPolicy {

    private static final int[] DELAYS = {30, 120, 300, 900, 1800};

    public int nextDelaySeconds(int attempt) {
        int index = Math.min(Math.max(attempt - 1, 0), DELAYS.length - 1);
        int base = DELAYS[index];
        int jitter = ThreadLocalRandom.current().nextInt((int) Math.round(base * 0.2) + 1);
        return base + jitter;
    }
}
