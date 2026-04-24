package com.hjo2oa.portal.widget.config.interfaces;

import com.hjo2oa.portal.widget.config.application.UpsertWidgetDefinitionCommand;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetDataSourceType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpsertWidgetDefinitionRequest(
        @NotBlank @Size(max = 128) String widgetCode,
        @NotBlank @Size(max = 128) String displayName,
        @NotNull WidgetCardType cardType,
        WidgetSceneType sceneType,
        @NotBlank @Size(max = 64) String sourceModule,
        @NotNull WidgetDataSourceType dataSourceType,
        Boolean allowHide,
        Boolean allowCollapse,
        @Positive Integer maxItems
) {

    public UpsertWidgetDefinitionCommand toCommand(String widgetId) {
        return new UpsertWidgetDefinitionCommand(
                widgetId,
                widgetCode,
                displayName,
                cardType,
                sceneType,
                sourceModule,
                dataSourceType,
                Boolean.TRUE.equals(allowHide),
                Boolean.TRUE.equals(allowCollapse),
                maxItems == null ? 10 : maxItems
        );
    }
}
