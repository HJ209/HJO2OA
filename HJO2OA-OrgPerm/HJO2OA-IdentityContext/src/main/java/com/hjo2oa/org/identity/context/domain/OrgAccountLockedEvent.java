package com.hjo2oa.org.identity.context.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OrgAccountLockedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String accountId,
        String personId
) implements DomainEvent {

    public static final String EVENT_TYPE = "org.account.locked";

    public OrgAccountLockedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        accountId = requireText(accountId, "accountId");
        personId = requireText(personId, "personId");
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
