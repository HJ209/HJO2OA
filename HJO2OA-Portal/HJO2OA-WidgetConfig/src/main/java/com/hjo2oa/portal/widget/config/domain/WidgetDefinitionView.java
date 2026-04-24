package com.hjo2oa.portal.widget.config.domain;

import java.time.Instant;

public record WidgetDefinitionView(
        String widgetId,
        String tenantId,
        String widgetCode,
        String displayName,
        WidgetCardType cardType,
        WidgetSceneType sceneType,
        String sourceModule,
        WidgetDataSourceType dataSourceType,
        boolean allowHide,
        boolean allowCollapse,
        int maxItems,
        WidgetDefinitionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
