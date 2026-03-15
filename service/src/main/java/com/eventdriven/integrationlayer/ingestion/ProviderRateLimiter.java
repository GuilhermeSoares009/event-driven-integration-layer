package com.eventdriven.integrationlayer.ingestion;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProviderRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ProviderRateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private final Environment environment;

    public ProviderRateLimiter(StringRedisTemplate redisTemplate, Environment environment) {
        this.redisTemplate = redisTemplate;
        this.environment = environment;
    }

    public RateLimitDecision checkAndConsume(String provider, String ipAddress) {
        int limitPerMinute = resolveLimit(provider);
        long nowEpochSeconds = Instant.now().getEpochSecond();
        long minuteWindow = nowEpochSeconds / 60;

        String key = "rl:" + provider + ":" + ipAddress + ":" + minuteWindow;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);

            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(70));
            }

            if (currentCount != null && currentCount > limitPerMinute) {
                int retryAfter = (int) (60 - (nowEpochSeconds % 60));
                return RateLimitDecision.limited(retryAfter);
            }

            return RateLimitDecision.allowed();
        } catch (Exception exception) {
            log.warn("rate_limit.fallback provider={} ip={} error={}", provider, ipAddress, exception.getMessage());
            return RateLimitDecision.allowed();
        }
    }

    private int resolveLimit(String provider) {
        Integer providerLimit = environment.getProperty(
            "integration.rate-limits.providers." + provider,
            Integer.class
        );
        if (providerLimit != null && providerLimit > 0) {
            return providerLimit;
        }

        Integer defaultLimit = environment.getProperty("integration.rate-limits.default-per-minute", Integer.class, 60);
        return defaultLimit == null || defaultLimit <= 0 ? 60 : defaultLimit;
    }
}
