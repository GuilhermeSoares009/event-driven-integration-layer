package com.eventdriven.integrationlayer.ingestion;

import java.time.Duration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProviderCircuitBreaker {

    private final StringRedisTemplate redisTemplate;
    private final Environment environment;

    public ProviderCircuitBreaker(StringRedisTemplate redisTemplate, Environment environment) {
        this.redisTemplate = redisTemplate;
        this.environment = environment;
    }

    public boolean isOpen(String provider) {
        Boolean exists = redisTemplate.hasKey(openKey(provider));
        return Boolean.TRUE.equals(exists);
    }

    public void recordFailure(String provider) {
        int threshold = environment.getProperty("integration.circuit-breaker.failure-threshold", Integer.class, 5);
        int openSeconds = environment.getProperty("integration.circuit-breaker.open-seconds", Integer.class, 120);

        String failureKey = failureKey(provider);
        Long count = redisTemplate.opsForValue().increment(failureKey);
        redisTemplate.expire(failureKey, Duration.ofSeconds(openSeconds));

        if (count != null && count >= threshold) {
            redisTemplate.opsForValue().set(openKey(provider), "1", Duration.ofSeconds(openSeconds));
        }
    }

    public void recordSuccess(String provider) {
        redisTemplate.delete(failureKey(provider));
        redisTemplate.delete(openKey(provider));
    }

    private String failureKey(String provider) {
        return "cb:failures:" + provider;
    }

    private String openKey(String provider) {
        return "cb:open:" + provider;
    }
}
