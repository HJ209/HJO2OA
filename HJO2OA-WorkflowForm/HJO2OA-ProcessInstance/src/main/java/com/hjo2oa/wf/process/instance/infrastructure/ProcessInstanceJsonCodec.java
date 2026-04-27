package com.hjo2oa.wf.process.instance.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProcessInstanceJsonCodec {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<UUID>> UUID_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public ProcessInstanceJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize process instance JSON", ex);
        }
    }

    public List<String> readStringList(String value) {
        return read(value, STRING_LIST_TYPE, List.of());
    }

    public List<UUID> readUuidList(String value) {
        return read(value, UUID_LIST_TYPE, List.of());
    }

    public Map<String, Object> readMap(String value) {
        return read(value, MAP_TYPE, Map.of());
    }

    private <T> T read(String value, TypeReference<T> type, T defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to deserialize process instance JSON", ex);
        }
    }
}
