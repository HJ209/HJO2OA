package com.hjo2oa.org.identity.context.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record IdentityContextInvalidatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String personId,
        String accountId,
        String invalidatedAssignmentId,
        String fallbackAssignmentId,
        IdentityContextInvalidationReason reasonCode,
        boolean forceLogout,
        long permissionSnapshotVersion,
        String triggerEvent
) implements DomainEvent {

    public static final String EVENT_TYPE = "org.identity-context.invalidated";

    public IdentityContextInvalidatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        personId = requireText(personId, "personId");
        accountId = requireText(accountId, "accountId");
        invalidatedAssignmentId = requireText(invalidatedAssignmentId, "invalidatedAssignmentId");
        Objects.requireNonNull(reasonCode, "reasonCode must not be null");
        triggerEvent = requireText(triggerEvent, "triggerEvent");
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
