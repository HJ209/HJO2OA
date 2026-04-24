package com.hjo2oa.infra.config.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ConfigUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        UUID configEntryId,
        String configKey,
        String changeType,
        OverrideScopeType scopeType,
        UUID scopeId
) implements DomainEvent {

    public static final String EVENT_TYPE = "infra.config.updated";

    public ConfigUpdatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(configEntryId, "configEntryId must not be null");
        configKey = requireText(configKey, "configKey");
        changeType = requireText(changeType, "changeType");
    }

    public static ConfigUpdatedEvent forDefaultUpdate(
            ConfigEntry entry,
            String changeType,
            Instant occurredAt
    ) {
        return new ConfigUpdatedEvent(
                UUID.randomUUID(),
                occurredAt,
                null,
                entry.id(),
                entry.configKey(),
                changeType,
                null,
                null
        );
    }

    public static ConfigUpdatedEvent forScopedUpdate(
            ConfigEntry entry,
            String changeType,
            OverrideScopeType scopeType,
            UUID scopeId,
            Instant occurredAt
    ) {
        return new ConfigUpdatedEvent(
                UUID.randomUUID(),
                occurredAt,
                scopeType == OverrideScopeType.TENANT && scopeId != null ? scopeId.toString() : null,
                entry.id(),
                entry.configKey(),
                changeType,
                scopeType,
                scopeId
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
