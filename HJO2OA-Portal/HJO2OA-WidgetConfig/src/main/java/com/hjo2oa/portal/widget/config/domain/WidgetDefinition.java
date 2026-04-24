package com.hjo2oa.portal.widget.config.domain;

import java.time.Instant;
import java.util.Objects;

public record WidgetDefinition(
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

    public WidgetDefinition {
        widgetId = requireText(widgetId, "widgetId");
        tenantId = requireText(tenantId, "tenantId");
        widgetCode = requireText(widgetCode, "widgetCode");
        displayName = requireText(displayName, "displayName");
        Objects.requireNonNull(cardType, "cardType must not be null");
        sourceModule = requireText(sourceModule, "sourceModule");
        Objects.requireNonNull(dataSourceType, "dataSourceType must not be null");
        if (maxItems <= 0) {
            throw new IllegalArgumentException("maxItems must be greater than 0");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static WidgetDefinition create(
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
            Instant now
    ) {
        return new WidgetDefinition(
                widgetId,
                tenantId,
                widgetCode,
                displayName,
                cardType,
                sceneType,
                sourceModule,
                dataSourceType,
                allowHide,
                allowCollapse,
                maxItems,
                WidgetDefinitionStatus.ACTIVE,
                now,
                now
        );
    }

    public WidgetDefinition update(
            String widgetCode,
            String displayName,
            WidgetCardType cardType,
            WidgetSceneType sceneType,
            String sourceModule,
            WidgetDataSourceType dataSourceType,
            boolean allowHide,
            boolean allowCollapse,
            int maxItems,
            Instant now
    ) {
        return new WidgetDefinition(
                widgetId,
                tenantId,
                widgetCode,
                displayName,
                cardType,
                sceneType,
                sourceModule,
                dataSourceType,
                allowHide,
                allowCollapse,
                maxItems,
                WidgetDefinitionStatus.ACTIVE,
                createdAt,
                now
        );
    }

    public WidgetDefinition disable(Instant now) {
        return new WidgetDefinition(
                widgetId,
                tenantId,
                widgetCode,
                displayName,
                cardType,
                sceneType,
                sourceModule,
                dataSourceType,
                allowHide,
                allowCollapse,
                maxItems,
                WidgetDefinitionStatus.DISABLED,
                createdAt,
                now
        );
    }

    public WidgetDefinitionView toView() {
        return new WidgetDefinitionView(
                widgetId,
                tenantId,
                widgetCode,
                displayName,
                cardType,
                sceneType,
                sourceModule,
                dataSourceType,
                allowHide,
                allowCollapse,
                maxItems,
                status,
                createdAt,
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
