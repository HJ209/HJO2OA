package com.hjo2oa.portal.portal.designer.domain;

import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PortalDesignerWidgetPaletteProjection(
        String widgetId,
        String widgetCode,
        WidgetCardType cardType,
        WidgetSceneType sceneType,
        PortalDesignerWidgetPaletteEntryState state,
        List<String> changedFields,
        String triggerEventType,
        Instant updatedAt
) {

    public PortalDesignerWidgetPaletteProjection {
        widgetId = requireText(widgetId, "widgetId");
        widgetCode = requireText(widgetCode, "widgetCode");
        Objects.requireNonNull(cardType, "cardType must not be null");
        Objects.requireNonNull(state, "state must not be null");
        changedFields = List.copyOf(Objects.requireNonNull(changedFields, "changedFields must not be null"));
        triggerEventType = requireText(triggerEventType, "triggerEventType");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static PortalDesignerWidgetPaletteProjection active(PortalWidgetUpdatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return new PortalDesignerWidgetPaletteProjection(
                event.widgetId(),
                event.widgetCode(),
                event.cardType(),
                event.sceneType(),
                PortalDesignerWidgetPaletteEntryState.ACTIVE,
                event.changedFields(),
                event.eventType(),
                event.occurredAt()
        );
    }

    public static PortalDesignerWidgetPaletteProjection disabled(PortalWidgetDisabledEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return new PortalDesignerWidgetPaletteProjection(
                event.widgetId(),
                event.widgetCode(),
                event.cardType(),
                event.sceneType(),
                PortalDesignerWidgetPaletteEntryState.DISABLED,
                List.of(),
                event.eventType(),
                event.occurredAt()
        );
    }

    public PortalDesignerWidgetPaletteEntryView toView() {
        return new PortalDesignerWidgetPaletteEntryView(
                widgetId,
                widgetCode,
                cardType,
                sceneType,
                state,
                changedFields,
                triggerEventType,
                updatedAt
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
