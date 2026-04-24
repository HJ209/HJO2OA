package com.hjo2oa.portal.widget.config.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PortalWidgetUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String widgetId,
        String widgetCode,
        WidgetCardType cardType,
        WidgetSceneType sceneType,
        WidgetCardType previousCardType,
        WidgetSceneType previousSceneType,
        List<String> changedFields
) implements DomainEvent {

    public static final String EVENT_TYPE = "portal.widget.updated";

    public PortalWidgetUpdatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        widgetId = requireText(widgetId, "widgetId");
        widgetCode = requireText(widgetCode, "widgetCode");
        Objects.requireNonNull(cardType, "cardType must not be null");
        changedFields = List.copyOf(Objects.requireNonNull(changedFields, "changedFields must not be null"));
    }

    public PortalWidgetUpdatedEvent(
            UUID eventId,
            Instant occurredAt,
            String tenantId,
            String widgetId,
            String widgetCode,
            WidgetCardType cardType,
            WidgetSceneType sceneType,
            List<String> changedFields
    ) {
        this(
                eventId,
                occurredAt,
                tenantId,
                widgetId,
                widgetCode,
                cardType,
                sceneType,
                null,
                null,
                changedFields
        );
    }

    public static PortalWidgetUpdatedEvent from(
            WidgetDefinition previousWidgetDefinition,
            WidgetDefinition widgetDefinition,
            List<String> changedFields,
            Instant occurredAt
    ) {
        return new PortalWidgetUpdatedEvent(
                UUID.randomUUID(),
                occurredAt,
                widgetDefinition.tenantId(),
                widgetDefinition.widgetId(),
                widgetDefinition.widgetCode(),
                widgetDefinition.cardType(),
                widgetDefinition.sceneType(),
                previousWidgetDefinition == null ? null : previousWidgetDefinition.cardType(),
                previousWidgetDefinition == null ? null : previousWidgetDefinition.sceneType(),
                changedFields
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
