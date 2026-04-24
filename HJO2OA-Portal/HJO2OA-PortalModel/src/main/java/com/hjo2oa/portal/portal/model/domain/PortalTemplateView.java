package com.hjo2oa.portal.portal.model.domain;

import java.time.Instant;
import java.util.List;

public record PortalTemplateView(
        String templateId,
        String tenantId,
        String templateCode,
        String displayName,
        PortalPublicationSceneType sceneType,
        Integer latestVersionNo,
        Integer publishedVersionNo,
        Instant createdAt,
        Instant updatedAt,
        List<PortalTemplateVersionView> versions
) {
}
