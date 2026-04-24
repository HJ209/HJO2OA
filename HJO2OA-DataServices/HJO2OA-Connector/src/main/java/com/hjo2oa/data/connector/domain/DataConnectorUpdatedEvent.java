package com.hjo2oa.data.connector.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record DataConnectorUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String connectorId,
        String code,
        ConnectorType connectorType,
        ConnectorStatus status,
        long changeSequence,
        List<String> changedFields
) implements DomainEvent {

    public static final String EVENT_TYPE = "data.connector.updated";

    public DataConnectorUpdatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        connectorId = requireText(connectorId, "connectorId");
        code = requireText(code, "code");
        Objects.requireNonNull(connectorType, "connectorType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (changeSequence < 0) {
            throw new IllegalArgumentException("changeSequence must not be negative");
        }
        changedFields = List.copyOf(Objects.requireNonNull(changedFields, "changedFields must not be null"));
    }

    public static DataConnectorUpdatedEvent from(
            ConnectorDefinition connectorDefinition,
            List<String> changedFields,
            Instant occurredAt
    ) {
        return new DataConnectorUpdatedEvent(
                UUID.randomUUID(),
                occurredAt,
                connectorDefinition.tenantId(),
                connectorDefinition.connectorId(),
                connectorDefinition.code(),
                connectorDefinition.connectorType(),
                connectorDefinition.status(),
                connectorDefinition.changeSequence(),
                changedFields
        );
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
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
