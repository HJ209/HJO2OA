package com.hjo2oa.portal.portal.model.domain;

import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PortalWidgetReferenceStatus(
        String widgetId,
        String tenantId,
        String widgetCode,
        WidgetCardType cardType,
        WidgetSceneType sceneType,
        PortalWidgetReferenceState state,
        List<String> changedFields,
        String triggerEventType,
        Instant updatedAt
) {

    public PortalWidgetReferenceStatus {
        widgetId = requireText(widgetId, "widgetId");
        tenantId = requireText(tenantId, "tenantId");
        widgetCode = requireText(widgetCode, "widgetCode");
        Objects.requireNonNull(cardType, "cardType must not be null");
        Objects.requireNonNull(state, "state must not be null");
        changedFields = List.copyOf(Objects.requireNonNull(changedFields, "changedFields must not be null"));
        triggerEventType = requireText(triggerEventType, "triggerEventType");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static PortalWidgetReferenceStatus stale(PortalWidgetUpdatedEvent event) {
        return new PortalWidgetReferenceStatus(
                event.widgetId(),
                event.tenantId(),
                event.widgetCode(),
                event.cardType(),
                event.sceneType(),
                PortalWidgetReferenceState.STALE,
                event.changedFields(),
                event.eventType(),
                event.occurredAt()
        );
    }

    public static PortalWidgetReferenceStatus repairRequired(PortalWidgetDisabledEvent event) {
        return new PortalWidgetReferenceStatus(
                event.widgetId(),
                event.tenantId(),
                event.widgetCode(),
                event.cardType(),
                event.sceneType(),
                PortalWidgetReferenceState.REPAIR_REQUIRED,
                List.of(),
                event.eventType(),
                event.occurredAt()
        );
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
