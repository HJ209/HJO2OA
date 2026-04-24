package com.hjo2oa.data.report.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ReportJsonCodec {

    private final ObjectMapper objectMapper;

    public ReportJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize report json payload", ex);
        }
    }

    public <T> T read(String value, Class<T> targetType) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, targetType);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to deserialize report json payload", ex);
        }
    }
}
