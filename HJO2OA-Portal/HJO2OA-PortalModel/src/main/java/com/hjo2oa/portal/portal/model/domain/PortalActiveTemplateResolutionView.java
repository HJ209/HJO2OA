package com.hjo2oa.portal.portal.model.domain;

import java.time.Instant;

public record PortalActiveTemplateResolutionView(
        String publicationId,
        String templateId,
        String templateCode,
        String templateDisplayName,
        PortalPublicationSceneType sceneType,
        PortalPublicationClientType clientType,
        PortalPublicationAudience audience,
        PortalPublicationStatus publicationStatus,
        Integer latestVersionNo,
        Integer publishedVersionNo,
        Instant publicationActivatedAt,
        Instant publicationUpdatedAt,
        Instant templateUpdatedAt
) {
}
