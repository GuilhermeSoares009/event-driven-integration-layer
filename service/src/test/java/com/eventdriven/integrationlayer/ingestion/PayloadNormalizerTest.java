package com.eventdriven.integrationlayer.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PayloadNormalizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PayloadNormalizer payloadNormalizer = new PayloadNormalizer(objectMapper);

    @Test
    void shouldNormalizeNestedObjectsBySortingKeys() throws Exception {
        JsonNode payload = objectMapper.readTree(
            "{\"b\":1,\"a\":{\"z\":9,\"c\":3},\"arr\":[{\"y\":2,\"x\":1}]}"
        );

        String normalized = payloadNormalizer.normalize(payload);

        assertEquals("{\"a\":{\"c\":3,\"z\":9},\"arr\":[{\"x\":1,\"y\":2}],\"b\":1}", normalized);
    }

    @Test
    void shouldKeepArrayOrder() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"items\":[3,2,1]}");

        String normalized = payloadNormalizer.normalize(payload);

        assertEquals("{\"items\":[3,2,1]}", normalized);
    }
}
