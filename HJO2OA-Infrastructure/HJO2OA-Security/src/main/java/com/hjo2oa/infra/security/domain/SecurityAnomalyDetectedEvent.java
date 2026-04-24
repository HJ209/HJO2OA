package com.hjo2oa.infra.security.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SecurityAnomalyDetectedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String policyCode,
        RateLimitSubjectType subjectType
) implements DomainEvent {

    public static final String EVENT_TYPE = "infra.security.anomaly-detected";

    public SecurityAnomalyDetectedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        policyCode = requireText(policyCode, "policyCode");
        Objects.requireNonNull(subjectType, "subjectType must not be null");
    }

    public static SecurityAnomalyDetectedEvent of(
            String policyCode,
            RateLimitSubjectType subjectType,
            String tenantId,
            Instant occurredAt
    ) {
        return new SecurityAnomalyDetectedEvent(
                UUID.randomUUID(),
                Objects.requireNonNull(occurredAt, "occurredAt must not be null"),
                tenantId,
                policyCode,
                subjectType
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
