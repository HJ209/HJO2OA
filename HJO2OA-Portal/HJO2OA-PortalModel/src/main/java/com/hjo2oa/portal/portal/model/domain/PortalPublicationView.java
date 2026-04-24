package com.hjo2oa.portal.portal.model.domain;

import java.time.Instant;

public record PortalPublicationView(
        String publicationId,
        String tenantId,
        String templateId,
        PortalPublicationSceneType sceneType,
        PortalPublicationClientType clientType,
        PortalPublicationAudience audience,
        PortalPublicationStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt,
        Instant offlinedAt
) {
}
