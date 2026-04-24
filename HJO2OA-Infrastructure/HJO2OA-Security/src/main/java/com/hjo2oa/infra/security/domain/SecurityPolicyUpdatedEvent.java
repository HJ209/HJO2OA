package com.hjo2oa.infra.security.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SecurityPolicyUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String policyCode,
        SecurityPolicyType policyType
) implements DomainEvent {

    public static final String EVENT_TYPE = "infra.security.policy-updated";

    public SecurityPolicyUpdatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        policyCode = requireText(policyCode, "policyCode");
        Objects.requireNonNull(policyType, "policyType must not be null");
    }

    public static SecurityPolicyUpdatedEvent from(SecurityPolicy securityPolicy, Instant occurredAt) {
        return new SecurityPolicyUpdatedEvent(
                UUID.randomUUID(),
                Objects.requireNonNull(occurredAt, "occurredAt must not be null"),
                securityPolicy.tenantId() == null ? null : securityPolicy.tenantId().toString(),
                securityPolicy.policyCode(),
                securityPolicy.policyType()
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
