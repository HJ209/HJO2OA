package com.hjo2oa.portal.portal.designer.domain;

import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCanvasView;

public record PortalDesignerTemplateInitializationView(
        String templateId,
        String templateCode,
        PortalPublicationSceneType sceneType,
        PortalDesignerTemplateStatusView status,
        PortalTemplateCanvasView canvas,
        PortalDesignerTemplateWidgetPaletteView widgetPalette
) {
}
