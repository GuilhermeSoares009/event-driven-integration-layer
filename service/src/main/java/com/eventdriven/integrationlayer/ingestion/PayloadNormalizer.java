package com.eventdriven.integrationlayer.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PayloadNormalizer {

    private final ObjectMapper objectMapper;

    public PayloadNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String normalize(JsonNode payload) {
        JsonNode normalized = normalizeNode(payload);
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to normalize payload", exception);
        }
    }

    private JsonNode normalizeNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return objectMapper.nullNode();
        }

        if (node.isObject()) {
            return normalizeObject(node);
        }

        if (node.isArray()) {
            return normalizeArray(node);
        }

        return node;
    }

    private JsonNode normalizeObject(JsonNode objectNode) {
        ObjectNode sortedObject = objectMapper.createObjectNode();
        List<String> fields = new ArrayList<>();

        Iterator<String> iterator = objectNode.fieldNames();
        while (iterator.hasNext()) {
            fields.add(iterator.next());
        }

        fields.sort(String::compareTo);
        for (String field : fields) {
            sortedObject.set(field, normalizeNode(objectNode.get(field)));
        }

        return sortedObject;
    }

    private JsonNode normalizeArray(JsonNode arrayNode) {
        ArrayNode normalizedArray = objectMapper.createArrayNode();
        for (JsonNode item : arrayNode) {
            normalizedArray.add(normalizeNode(item));
        }
        return normalizedArray;
    }
}
