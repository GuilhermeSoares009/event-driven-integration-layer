package com.eventdriven.integrationlayer.ingestion;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {

    private final WebhookIngestionService ingestionService;
    private final ProviderRateLimiter rateLimiter;

    public WebhookController(WebhookIngestionService ingestionService, ProviderRateLimiter rateLimiter) {
        this.ingestionService = ingestionService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping({"/webhooks/{provider}/{topic}", "/api/v1/webhooks/{provider}/{topic}"})
    public ResponseEntity<Map<String, Object>> ingest(
        @PathVariable String provider,
        @PathVariable String topic,
        @RequestHeader(value = "X-Signature", required = false) String signature,
        @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
        @RequestBody(required = false) String payloadRaw,
        HttpServletRequest request
    ) {
        String clientIp = extractClientIp(request);
        RateLimitDecision rateLimit = rateLimiter.checkAndConsume(provider, clientIp);
        if (rateLimit.limited()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(rateLimit.retryAfterSeconds()))
                .body(Map.of(
                    "error", "rate_limited",
                    "retry_after", rateLimit.retryAfterSeconds()
                ));
        }

        WebhookIngestionResponse result = ingestionService.ingest(
            provider,
            topic,
            payloadRaw,
            signature,
            correlationId
        );

        return switch (result.outcome()) {
            case CIRCUIT_OPEN -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "circuit_open"));
            case ACCEPTED -> ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(acceptedBody(false, result.correlationId()));
            case DEDUPED -> ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(acceptedBody(true, result.correlationId()));
            case INVALID_SIGNATURE -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "invalid_signature"));
            case INVALID_PAYLOAD -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "invalid_payload"));
        };
    }

    private Map<String, Object> acceptedBody(boolean deduped, String correlationId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "accepted");
        body.put("deduped", deduped);
        body.put("correlation_id", correlationId);
        return body;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIndex = forwardedFor.indexOf(',');
            return commaIndex >= 0
                ? forwardedFor.substring(0, commaIndex).trim()
                : forwardedFor.trim();
        }

        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
