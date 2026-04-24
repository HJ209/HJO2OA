package com.hjo2oa.portal.portal.designer.domain;

import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import java.time.Instant;
import java.util.List;

public record PortalDesignerTemplateStatusView(
        String templateId,
        String templateCode,
        PortalPublicationSceneType sceneType,
        Integer latestVersionNo,
        Integer publishedVersionNo,
        List<PortalDesignerTemplateVersionView> versions,
        List<String> activePublicationIds,
        boolean hasActivePublication,
        Instant createdAt,
        Instant updatedAt
) {
}
