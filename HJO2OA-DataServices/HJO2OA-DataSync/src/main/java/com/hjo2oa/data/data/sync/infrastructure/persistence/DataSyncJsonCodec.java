package com.hjo2oa.data.data.sync.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DataSyncJsonCodec {

    private final ObjectMapper objectMapper;

    public DataSyncJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize json payload", ex);
        }
    }

    public <T> T read(String json, Class<T> targetType) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to deserialize json payload", ex);
        }
    }

    public <T> T read(String json, TypeReference<T> typeReference) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to deserialize json payload", ex);
        }
    }
}
