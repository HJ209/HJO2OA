package com.hjo2oa.portal.portal.model.domain;

import java.util.List;

public record PortalTemplateCanvasView(
        String templateId,
        String templateCode,
        PortalPublicationSceneType sceneType,
        Integer latestVersionNo,
        Integer publishedVersionNo,
        List<PortalPageView> pages
) {
}
