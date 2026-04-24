package com.hjo2oa.org.identity.context.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OrgPositionDisabledEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String positionId
) implements DomainEvent {

    public static final String EVENT_TYPE = "org.position.disabled";

    public OrgPositionDisabledEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        positionId = requireText(positionId, "positionId");
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
