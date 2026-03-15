package com.eventdriven.integrationlayer.ingestion;

import com.eventdriven.integrationlayer.inbox.InboxEvent;
import com.eventdriven.integrationlayer.inbox.InboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WebhookIngestionService {

    private static final Logger log = LoggerFactory.getLogger(WebhookIngestionService.class);

    private final WebhookSignatureValidator signatureValidator;
    private final PayloadNormalizer payloadNormalizer;
    private final InboxEventRepository inboxEventRepository;
    private final ObjectMapper objectMapper;

    public WebhookIngestionService(
        WebhookSignatureValidator signatureValidator,
        PayloadNormalizer payloadNormalizer,
        InboxEventRepository inboxEventRepository,
        ObjectMapper objectMapper
    ) {
        this.signatureValidator = signatureValidator;
        this.payloadNormalizer = payloadNormalizer;
        this.inboxEventRepository = inboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public WebhookIngestionResponse ingest(
        String provider,
        String topic,
        String payloadRaw,
        String signature,
        String providedCorrelationId
    ) {
        if (!signatureValidator.isValid(provider, payloadRaw, signature)) {
            log.warn("webhook.invalid_signature provider={} topic={}", provider, topic);
            return WebhookIngestionResponse.invalidSignature();
        }

        JsonNode payload = parsePayload(payloadRaw);
        if (payload == null) {
            return WebhookIngestionResponse.invalidPayload();
        }

        String externalEventId = extractExternalEventId(payload);
        String normalizedPayload = payloadNormalizer.normalize(payload);
        String payloadHash = sha256Hex(normalizedPayload);
        String correlationId = resolveCorrelationId(providedCorrelationId);

        if (isDuplicate(provider, externalEventId, payloadHash)) {
            return WebhookIngestionResponse.deduped(correlationId);
        }

        try {
            InboxEvent event = buildInboxEvent(provider, topic, externalEventId, payloadHash, payload, correlationId);
            InboxEvent saved = inboxEventRepository.save(event);

            log.info(
                "webhook.received provider={} topic={} correlation_id={} inbox_event_id={}",
                provider,
                topic,
                correlationId,
                saved.getId()
            );

            return WebhookIngestionResponse.accepted(correlationId, saved.getId());
        } catch (DataIntegrityViolationException exception) {
            return WebhookIngestionResponse.deduped(correlationId);
        }
    }

    private JsonNode parsePayload(String payloadRaw) {
        if (!StringUtils.hasText(payloadRaw)) {
            return null;
        }

        try {
            JsonNode payload = objectMapper.readTree(payloadRaw);
            return payload == null || payload.isNull() ? null : payload;
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private String extractExternalEventId(JsonNode payload) {
        if (!payload.isObject()) {
            return null;
        }

        JsonNode externalEventIdNode = payload.get("external_event_id");
        if (externalEventIdNode == null || !externalEventIdNode.isTextual()) {
            return null;
        }

        String externalEventId = externalEventIdNode.asText().trim();
        return externalEventId.isEmpty() ? null : externalEventId;
    }

    private String resolveCorrelationId(String providedCorrelationId) {
        if (StringUtils.hasText(providedCorrelationId)) {
            return providedCorrelationId.trim();
        }
        return UUID.randomUUID().toString();
    }

    private boolean isDuplicate(String provider, String externalEventId, String payloadHash) {
        if (externalEventId != null) {
            return inboxEventRepository.existsByProviderAndExternalEventId(provider, externalEventId);
        }

        return inboxEventRepository.existsByProviderAndExternalEventIdIsNullAndPayloadHash(provider, payloadHash);
    }

    private InboxEvent buildInboxEvent(
        String provider,
        String topic,
        String externalEventId,
        String payloadHash,
        JsonNode payload,
        String correlationId
    ) {
        InboxEvent event = new InboxEvent();
        event.setProvider(provider);
        event.setTopic(topic);
        event.setExternalEventId(externalEventId);
        event.setPayloadHash(payloadHash);
        event.setPayloadJson(writePayload(payload));
        event.setStatus(InboxEvent.STATUS_RECEIVED);
        event.setAttempts(0);
        event.setReceivedAt(OffsetDateTime.now(ZoneOffset.UTC));
        event.setCorrelationId(correlationId);
        return event;
    }

    private String writePayload(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize payload", exception);
        }
    }

    private String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest
                .getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to compute SHA-256", exception);
        }
    }
}
