package com.hjo2oa.portal.portal.designer.domain;

import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import java.time.Instant;
import java.util.List;

public record PortalDesignerWidgetPaletteEntryView(
        String widgetId,
        String widgetCode,
        WidgetCardType cardType,
        WidgetSceneType sceneType,
        PortalDesignerWidgetPaletteEntryState state,
        List<String> changedFields,
        String triggerEventType,
        Instant updatedAt
) {
}
