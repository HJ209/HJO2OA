package com.hjo2oa.portal.portal.designer.domain;

import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetDataSourceType;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionStatus;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;

public record PortalDesignerTemplateWidgetPaletteItemView(
        String widgetId,
        String widgetCode,
        String displayName,
        WidgetCardType cardType,
        WidgetSceneType sceneType,
        String sourceModule,
        WidgetDataSourceType dataSourceType,
        boolean allowHide,
        boolean allowCollapse,
        int maxItems,
        WidgetDefinitionStatus status
) {
}
