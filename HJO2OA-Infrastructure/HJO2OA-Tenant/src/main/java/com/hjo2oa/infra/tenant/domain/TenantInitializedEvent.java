package com.hjo2oa.infra.tenant.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TenantInitializedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId
) implements DomainEvent {

    public static final String EVENT_TYPE = "infra.tenant.initialized";

    public TenantInitializedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
    }

    public static TenantInitializedEvent from(TenantProfile profile, Instant occurredAt) {
        return new TenantInitializedEvent(UUID.randomUUID(), occurredAt, profile.id().toString());
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
