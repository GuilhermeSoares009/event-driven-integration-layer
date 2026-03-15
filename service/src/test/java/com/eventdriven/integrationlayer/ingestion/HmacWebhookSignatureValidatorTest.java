package com.eventdriven.integrationlayer.ingestion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class HmacWebhookSignatureValidatorTest {

    @Test
    void shouldValidatePlainAndPrefixedSignature() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("integration.providers.test.secret", "secret");

        ProviderSecretResolver resolver = new ProviderSecretResolver(environment);
        HmacWebhookSignatureValidator validator = new HmacWebhookSignatureValidator(resolver);

        String payload = "{\"foo\":\"bar\"}";
        String signature = hmacSha256(payload, "secret");

        assertTrue(validator.isValid("test", payload, signature));
        assertTrue(validator.isValid("test", payload, "sha256=" + signature));
    }

    @Test
    void shouldRejectInvalidSignature() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("integration.providers.test.secret", "secret");

        ProviderSecretResolver resolver = new ProviderSecretResolver(environment);
        HmacWebhookSignatureValidator validator = new HmacWebhookSignatureValidator(resolver);

        String payload = "{\"foo\":\"bar\"}";

        assertFalse(validator.isValid("test", payload, "wrong"));
        assertFalse(validator.isValid("unknown", payload, "anything"));
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
