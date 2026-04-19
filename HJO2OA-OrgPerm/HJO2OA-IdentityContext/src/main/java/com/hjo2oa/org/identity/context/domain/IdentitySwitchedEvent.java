package com.hjo2oa.org.identity.context.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record IdentitySwitchedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String personId,
        String accountId,
        String fromAssignmentId,
        String toAssignmentId,
        String fromPositionId,
        String toPositionId,
        IdentityAssignmentType fromAssignmentType,
        IdentityAssignmentType toAssignmentType,
        String reason
) implements DomainEvent {

    public static final String EVENT_TYPE = "org.identity.switched";

    public IdentitySwitchedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        personId = requireText(personId, "personId");
        accountId = requireText(accountId, "accountId");
        fromAssignmentId = requireText(fromAssignmentId, "fromAssignmentId");
        toAssignmentId = requireText(toAssignmentId, "toAssignmentId");
        fromPositionId = requireText(fromPositionId, "fromPositionId");
        toPositionId = requireText(toPositionId, "toPositionId");
        Objects.requireNonNull(fromAssignmentType, "fromAssignmentType must not be null");
        Objects.requireNonNull(toAssignmentType, "toAssignmentType must not be null");
        reason = normalize(reason);
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

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
