package com.hjo2oa.infra.tenant.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TenantCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String tenantCode
) implements DomainEvent {

    public static final String EVENT_TYPE = "infra.tenant.created";

    public TenantCreatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        tenantCode = requireText(tenantCode, "tenantCode");
    }

    public static TenantCreatedEvent from(TenantProfile profile, Instant occurredAt) {
        return new TenantCreatedEvent(
                UUID.randomUUID(),
                occurredAt,
                profile.id().toString(),
                profile.tenantCode()
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
