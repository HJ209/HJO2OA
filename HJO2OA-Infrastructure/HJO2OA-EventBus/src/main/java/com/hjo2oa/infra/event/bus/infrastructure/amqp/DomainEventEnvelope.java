package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DomainEventEnvelope(
        UUID eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String tenantId,
        Instant occurredAt,
        String traceId,
        String schemaVersion,
        JsonNode payload,
        String eventClass,
        JsonNode eventBody
) {

    public DomainEventEnvelope {
        Objects.requireNonNull(eventId, "eventId must not be null");
        eventType = requireText(eventType, "eventType");
        aggregateType = requireText(aggregateType, "aggregateType");
        aggregateId = requireText(aggregateId, "aggregateId");
        tenantId = requireText(tenantId, "tenantId");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        traceId = requireText(traceId, "traceId");
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        Objects.requireNonNull(payload, "payload must not be null");
        eventClass = eventClass == null ? null : eventClass.trim();
        Objects.requireNonNull(eventBody, "eventBody must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
