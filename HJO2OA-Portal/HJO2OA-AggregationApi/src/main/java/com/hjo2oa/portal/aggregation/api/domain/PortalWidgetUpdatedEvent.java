package com.hjo2oa.portal.aggregation.api.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PortalWidgetUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String widgetCode,
        PortalCardType cardType,
        PortalSceneType sceneType
) implements DomainEvent {

    public static final String EVENT_TYPE = "portal.widget.updated";

    public PortalWidgetUpdatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        widgetCode = requireText(widgetCode, "widgetCode");
        Objects.requireNonNull(cardType, "cardType must not be null");
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
