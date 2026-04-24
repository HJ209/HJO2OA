package com.hjo2oa.data.data.sync.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DomainEventPayloadExtractor {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public DomainEventPayloadExtractor(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public Map<String, Object> payload(DomainEvent event) {
        if (event == null) {
            return Map.of();
        }
        if (event instanceof com.hjo2oa.data.common.domain.event.DataDomainEvent dataDomainEvent) {
            return dataDomainEvent.payload();
        }
        return objectMapper.convertValue(event, MAP_TYPE);
    }

    public String text(DomainEvent event, String fieldName) {
        Object value = payload(event).get(fieldName);
        return value == null ? null : String.valueOf(value);
    }

    public UUID uuid(DomainEvent event, String fieldName) {
        String value = text(event, fieldName);
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }
}
