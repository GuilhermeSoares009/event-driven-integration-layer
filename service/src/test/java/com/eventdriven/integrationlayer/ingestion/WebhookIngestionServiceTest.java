package com.eventdriven.integrationlayer.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eventdriven.integrationlayer.inbox.InboxEvent;
import com.eventdriven.integrationlayer.inbox.InboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookIngestionServiceTest {

    @Mock
    private WebhookSignatureValidator signatureValidator;

    @Mock
    private InboxEventRepository inboxEventRepository;

    @Mock
    private ProviderCircuitBreaker circuitBreaker;

    private WebhookIngestionService webhookIngestionService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        PayloadNormalizer payloadNormalizer = new PayloadNormalizer(objectMapper);
        webhookIngestionService = new WebhookIngestionService(
            signatureValidator,
            payloadNormalizer,
            inboxEventRepository,
            objectMapper,
            circuitBreaker
        );

        when(circuitBreaker.isOpen(anyString())).thenReturn(false);
    }

    @Test
    void shouldReturnCircuitOpenWhenProviderIsBlocked() {
        when(circuitBreaker.isOpen("test")).thenReturn(true);

        WebhookIngestionResponse result = webhookIngestionService.ingest(
            "test",
            "orders",
            "{\"foo\":\"bar\"}",
            "sig",
            null
        );

        assertEquals(WebhookIngestionOutcome.CIRCUIT_OPEN, result.outcome());
        verifyNoInteractions(signatureValidator);
        verifyNoInteractions(inboxEventRepository);
    }

    @Test
    void shouldReturnInvalidSignatureWhenSignatureIsNotValid() {
        when(signatureValidator.isValid("test", "{\"foo\":\"bar\"}", "sig")).thenReturn(false);

        WebhookIngestionResponse result = webhookIngestionService.ingest(
            "test",
            "orders",
            "{\"foo\":\"bar\"}",
            "sig",
            null
        );

        assertEquals(WebhookIngestionOutcome.INVALID_SIGNATURE, result.outcome());
        assertNull(result.correlationId());
        verifyNoInteractions(inboxEventRepository);
        verify(circuitBreaker).recordFailure("test");
    }

    @Test
    void shouldReturnInvalidPayloadWhenBodyIsMalformed() {
        when(signatureValidator.isValid("test", "not-json", "sig")).thenReturn(true);

        WebhookIngestionResponse result = webhookIngestionService.ingest(
            "test",
            "orders",
            "not-json",
            "sig",
            null
        );

        assertEquals(WebhookIngestionOutcome.INVALID_PAYLOAD, result.outcome());
        assertNull(result.correlationId());
        verifyNoInteractions(inboxEventRepository);
    }

    @Test
    void shouldReturnDedupedWhenExternalEventIdAlreadyExists() {
        String payload = "{\"external_event_id\":\"evt_1\",\"foo\":\"bar\"}";

        when(signatureValidator.isValid("test", payload, "sig")).thenReturn(true);
        when(inboxEventRepository.existsByProviderAndExternalEventId("test", "evt_1")).thenReturn(true);

        WebhookIngestionResponse result = webhookIngestionService.ingest(
            "test",
            "orders",
            payload,
            "sig",
            null
        );

        assertEquals(WebhookIngestionOutcome.DEDUPED, result.outcome());
        assertNotNull(result.correlationId());
        verify(inboxEventRepository, never()).save(any(InboxEvent.class));
        verify(circuitBreaker).recordSuccess("test");
    }

    @Test
    void shouldReturnDedupedWhenPayloadHashAlreadyExistsWithoutExternalId() {
        String payload = "{\"foo\":\"bar\"}";

        when(signatureValidator.isValid("test", payload, "sig")).thenReturn(true);
        when(inboxEventRepository.existsByProviderAndExternalEventIdIsNullAndPayloadHash(eq("test"), anyString()))
            .thenReturn(true);

        WebhookIngestionResponse result = webhookIngestionService.ingest(
            "test",
            "orders",
            payload,
            "sig",
            null
        );

        assertEquals(WebhookIngestionOutcome.DEDUPED, result.outcome());
        verify(inboxEventRepository, never()).save(any(InboxEvent.class));
        verify(circuitBreaker).recordSuccess("test");
    }

    @Test
    void shouldPersistEventWhenPayloadIsValidAndNotDuplicated() {
        String payload = "{\"external_event_id\":\"evt_2\",\"foo\":\"bar\"}";

        when(signatureValidator.isValid("test", payload, "sig")).thenReturn(true);
        when(inboxEventRepository.existsByProviderAndExternalEventId("test", "evt_2")).thenReturn(false);
        when(inboxEventRepository.save(any(InboxEvent.class))).thenAnswer(invocation -> {
            InboxEvent event = invocation.getArgument(0);
            event.setId(99L);
            return event;
        });

        WebhookIngestionResponse result = webhookIngestionService.ingest(
            "test",
            "orders",
            payload,
            "sig",
            "corr-1"
        );

        assertEquals(WebhookIngestionOutcome.ACCEPTED, result.outcome());
        assertEquals("corr-1", result.correlationId());
        assertEquals(99L, result.inboxEventId());

        ArgumentCaptor<InboxEvent> eventCaptor = ArgumentCaptor.forClass(InboxEvent.class);
        verify(inboxEventRepository).save(eventCaptor.capture());

        InboxEvent saved = eventCaptor.getValue();
        assertEquals("test", saved.getProvider());
        assertEquals("orders", saved.getTopic());
        assertEquals("evt_2", saved.getExternalEventId());
        assertEquals(InboxEvent.STATUS_RECEIVED, saved.getStatus());
        assertEquals(0, saved.getAttempts());
        assertEquals("corr-1", saved.getCorrelationId());
        assertNotNull(saved.getPayloadHash());
        assertNotNull(saved.getPayloadJson());
        verify(circuitBreaker).recordSuccess("test");
    }
}
