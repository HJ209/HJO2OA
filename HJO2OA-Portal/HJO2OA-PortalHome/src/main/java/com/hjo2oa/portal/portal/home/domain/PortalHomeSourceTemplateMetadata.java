package com.hjo2oa.portal.portal.home.domain;

import com.hjo2oa.portal.portal.model.domain.PortalActiveTemplateResolutionView;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationStatus;
import java.time.Instant;
import java.util.Objects;

public record PortalHomeSourceTemplateMetadata(
        String publicationId,
        String templateId,
        String templateCode,
        String templateDisplayName,
        PortalPublicationSceneType sceneType,
        PortalPublicationClientType clientType,
        PortalPublicationStatus publicationStatus,
        Integer latestVersionNo,
        Integer publishedVersionNo,
        Instant publicationActivatedAt,
        Instant publicationUpdatedAt,
        Instant templateUpdatedAt
) {

    public PortalHomeSourceTemplateMetadata {
        publicationId = requireText(publicationId, "publicationId");
        templateId = requireText(templateId, "templateId");
        templateCode = requireText(templateCode, "templateCode");
        templateDisplayName = requireText(templateDisplayName, "templateDisplayName");
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Objects.requireNonNull(clientType, "clientType must not be null");
        Objects.requireNonNull(publicationStatus, "publicationStatus must not be null");
    }

    public static PortalHomeSourceTemplateMetadata from(PortalActiveTemplateResolutionView resolution) {
        Objects.requireNonNull(resolution, "resolution must not be null");
        return new PortalHomeSourceTemplateMetadata(
                resolution.publicationId(),
                resolution.templateId(),
                resolution.templateCode(),
                resolution.templateDisplayName(),
                resolution.sceneType(),
                resolution.clientType(),
                resolution.publicationStatus(),
                resolution.latestVersionNo(),
                resolution.publishedVersionNo(),
                resolution.publicationActivatedAt(),
                resolution.publicationUpdatedAt(),
                resolution.templateUpdatedAt()
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
