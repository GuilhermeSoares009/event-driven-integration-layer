package com.eventdriven.integrationlayer.ingestion;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HmacWebhookSignatureValidator implements WebhookSignatureValidator {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    private final ProviderSecretResolver secretResolver;

    public HmacWebhookSignatureValidator(ProviderSecretResolver secretResolver) {
        this.secretResolver = secretResolver;
    }

    @Override
    public boolean isValid(String provider, String payload, String signature) {
        String secret = secretResolver.resolve(provider);
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(signature) || payload == null) {
            return false;
        }

        String normalizedSignature = signature.trim();
        if (normalizedSignature.startsWith("sha256=")) {
            normalizedSignature = normalizedSignature.substring(7);
        }

        String expected = hmacSha256Hex(payload, secret);
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            normalizedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to compute HMAC SHA-256", exception);
        }
    }
}
