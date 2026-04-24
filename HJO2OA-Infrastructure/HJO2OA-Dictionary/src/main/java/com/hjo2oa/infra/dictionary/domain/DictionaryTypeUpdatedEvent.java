package com.hjo2oa.infra.dictionary.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DictionaryTypeUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID tenantUuid,
        UUID dictionaryTypeId,
        String dictionaryCode
) implements DomainEvent {

    public static final String EVENT_TYPE = "infra.dictionary.updated";

    public DictionaryTypeUpdatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(dictionaryTypeId, "dictionaryTypeId must not be null");
        dictionaryCode = requireText(dictionaryCode, "dictionaryCode");
    }

    public static DictionaryTypeUpdatedEvent from(DictionaryType dictionaryType, Instant occurredAt) {
        return new DictionaryTypeUpdatedEvent(
                UUID.randomUUID(),
                occurredAt,
                dictionaryType.tenantId(),
                dictionaryType.id(),
                dictionaryType.code()
        );
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public String tenantId() {
        return tenantUuid == null ? null : tenantUuid.toString();
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
