package com.hjo2oa.infra.cache.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CacheInvalidatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String namespace,
        String invalidateKey,
        InvalidationReasonType reasonType
) implements DomainEvent {

    public static final String EVENT_TYPE = "infra.cache.invalidated";
    public static final String GLOBAL_TENANT_ID = "GLOBAL";

    public CacheInvalidatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        namespace = requireText(namespace, "namespace");
        invalidateKey = requireText(invalidateKey, "invalidateKey");
        Objects.requireNonNull(reasonType, "reasonType must not be null");
    }

    public static CacheInvalidatedEvent of(
            String namespace,
            String invalidateKey,
            InvalidationReasonType reasonType,
            Instant occurredAt
    ) {
        return new CacheInvalidatedEvent(
                UUID.randomUUID(),
                occurredAt,
                GLOBAL_TENANT_ID,
                namespace,
                invalidateKey,
                reasonType
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
