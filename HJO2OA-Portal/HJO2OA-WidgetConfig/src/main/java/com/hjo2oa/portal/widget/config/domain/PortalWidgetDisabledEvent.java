package com.hjo2oa.portal.widget.config.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PortalWidgetDisabledEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String widgetId,
        String widgetCode,
        WidgetCardType cardType,
        WidgetSceneType sceneType
) implements DomainEvent {

    public static final String EVENT_TYPE = "portal.widget.disabled";

    public PortalWidgetDisabledEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        widgetId = requireText(widgetId, "widgetId");
        widgetCode = requireText(widgetCode, "widgetCode");
        Objects.requireNonNull(cardType, "cardType must not be null");
    }

    public static PortalWidgetDisabledEvent from(WidgetDefinition widgetDefinition, Instant occurredAt) {
        return new PortalWidgetDisabledEvent(
                UUID.randomUUID(),
                occurredAt,
                widgetDefinition.tenantId(),
                widgetDefinition.widgetId(),
                widgetDefinition.widgetCode(),
                widgetDefinition.cardType(),
                widgetDefinition.sceneType()
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
