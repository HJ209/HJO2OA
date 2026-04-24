package com.hjo2oa.portal.portal.model.domain;

import java.time.Instant;
import java.util.Objects;

public record PortalPublication(
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

    public PortalPublication {
        publicationId = requireText(publicationId, "publicationId");
        tenantId = requireText(tenantId, "tenantId");
        templateId = requireText(templateId, "templateId");
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Objects.requireNonNull(clientType, "clientType must not be null");
        Objects.requireNonNull(audience, "audience must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (status == PortalPublicationStatus.ACTIVE) {
            Objects.requireNonNull(activatedAt, "activatedAt must not be null when publication is active");
        }
    }

    public static PortalPublication create(
            String publicationId,
            String tenantId,
            String templateId,
            PortalPublicationSceneType sceneType,
            PortalPublicationClientType clientType,
            PortalPublicationAudience audience,
            Instant now
    ) {
        return new PortalPublication(
                publicationId,
                tenantId,
                templateId,
                sceneType,
                clientType,
                audience,
                PortalPublicationStatus.ACTIVE,
                now,
                now,
                now,
                null
        );
    }

    public PortalPublication activate(
            String templateId,
            PortalPublicationSceneType sceneType,
            PortalPublicationClientType clientType,
            PortalPublicationAudience audience,
            Instant now
    ) {
        return new PortalPublication(
                publicationId,
                tenantId,
                templateId,
                sceneType,
                clientType,
                audience,
                PortalPublicationStatus.ACTIVE,
                createdAt,
                now,
                now,
                null
        );
    }

    public PortalPublication offline(Instant now) {
        return new PortalPublication(
                publicationId,
                tenantId,
                templateId,
                sceneType,
                clientType,
                audience,
                PortalPublicationStatus.OFFLINE,
                createdAt,
                now,
                activatedAt,
                now
        );
    }

    public PortalPublicationView toView() {
        return new PortalPublicationView(
                publicationId,
                tenantId,
                templateId,
                sceneType,
                clientType,
                audience,
                status,
                createdAt,
                updatedAt,
                activatedAt,
                offlinedAt
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
