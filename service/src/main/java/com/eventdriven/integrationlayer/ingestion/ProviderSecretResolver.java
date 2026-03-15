package com.eventdriven.integrationlayer.ingestion;

import java.util.Locale;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ProviderSecretResolver {

    private final Environment environment;

    public ProviderSecretResolver(Environment environment) {
        this.environment = environment;
    }

    public String resolve(String provider) {
        String propertyKey = "integration.providers." + provider + ".secret";
        String configured = environment.getProperty(propertyKey);
        if (StringUtils.hasText(configured)) {
            return configured.trim();
        }

        String normalizedProvider = provider
            .trim()
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]", "_");

        String envKey = "WEBHOOK_PROVIDER_" + normalizedProvider + "_SECRET";
        String envValue = environment.getProperty(envKey);
        if (StringUtils.hasText(envValue)) {
            return envValue.trim();
        }

        return null;
    }
}
