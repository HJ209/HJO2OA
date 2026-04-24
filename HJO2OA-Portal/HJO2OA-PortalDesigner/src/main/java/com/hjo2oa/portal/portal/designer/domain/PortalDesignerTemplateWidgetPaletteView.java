package com.hjo2oa.portal.portal.designer.domain;

import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import java.util.List;

public record PortalDesignerTemplateWidgetPaletteView(
        String templateId,
        String templateCode,
        PortalPublicationSceneType sceneType,
        List<PortalDesignerTemplateWidgetPaletteItemView> widgets
) {
}
