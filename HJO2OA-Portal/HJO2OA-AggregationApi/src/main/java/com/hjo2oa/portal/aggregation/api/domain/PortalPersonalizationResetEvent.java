package com.hjo2oa.portal.aggregation.api.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PortalPersonalizationResetEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String profileId,
        String personId,
        PortalSceneType sceneType
) implements DomainEvent {

    public static final String EVENT_TYPE = "portal.personalization.reset";

    public PortalPersonalizationResetEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        profileId = requireText(profileId, "profileId");
        personId = requireText(personId, "personId");
        Objects.requireNonNull(sceneType, "sceneType must not be null");
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
