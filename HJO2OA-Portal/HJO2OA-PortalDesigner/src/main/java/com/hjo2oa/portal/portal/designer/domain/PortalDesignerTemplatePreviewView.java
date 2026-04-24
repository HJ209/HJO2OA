package com.hjo2oa.portal.portal.designer.domain;

import com.hjo2oa.portal.portal.home.domain.PortalHomePageView;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import java.time.Instant;

public record PortalDesignerTemplatePreviewView(
        String templateId,
        String templateCode,
        String templateDisplayName,
        PortalPublicationSceneType sceneType,
        PortalPublicationClientType clientType,
        Integer latestVersionNo,
        Integer publishedVersionNo,
        PortalDesignerPreviewIdentityView previewIdentity,
        PortalDesignerPreviewOverlayView overlay,
        PortalHomePageView page,
        Instant previewedAt
) {
}
