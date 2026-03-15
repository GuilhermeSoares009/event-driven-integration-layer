package com.eventdriven.integrationlayer.ingestion;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = WebhookController.class)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebhookIngestionService ingestionService;

    @MockBean
    private ProviderRateLimiter rateLimiter;

    @Test
    void shouldReturnAcceptedResponse() throws Exception {
        when(rateLimiter.checkAndConsume(eq("test"), anyString()))
            .thenReturn(RateLimitDecision.allowed());
        when(ingestionService.ingest(eq("test"), eq("orders"), anyString(), eq("sig"), isNull()))
            .thenReturn(WebhookIngestionResponse.accepted("corr-1", 1L));

        mockMvc.perform(post("/api/v1/webhooks/test/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", "sig")
                .content("{\"foo\":\"bar\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("accepted"))
            .andExpect(jsonPath("$.deduped").value(false))
            .andExpect(jsonPath("$.correlation_id").value("corr-1"));
    }

    @Test
    void shouldReturnUnauthorizedWhenSignatureIsInvalid() throws Exception {
        when(rateLimiter.checkAndConsume(eq("test"), anyString()))
            .thenReturn(RateLimitDecision.allowed());
        when(ingestionService.ingest(eq("test"), eq("orders"), anyString(), eq("sig"), isNull()))
            .thenReturn(WebhookIngestionResponse.invalidSignature());

        mockMvc.perform(post("/webhooks/test/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", "sig")
                .content("{\"foo\":\"bar\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("invalid_signature"));
    }

    @Test
    void shouldReturnTooManyRequestsWhenRateLimitIsExceeded() throws Exception {
        when(rateLimiter.checkAndConsume(eq("test"), anyString()))
            .thenReturn(RateLimitDecision.limited(12));

        mockMvc.perform(post("/api/v1/webhooks/test/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", "sig")
                .content("{\"foo\":\"bar\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").value("rate_limited"))
            .andExpect(jsonPath("$.retry_after").value(12));

        verifyNoInteractions(ingestionService);
    }
}
