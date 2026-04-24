package com.hjo2oa.portal.portal.model.application;

import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import java.util.Objects;

public record ActivatePortalPublicationCommand(
        String publicationId,
        String templateId,
        PortalPublicationSceneType sceneType,
        PortalPublicationClientType clientType,
        PortalPublicationAudience audience
) {

    public ActivatePortalPublicationCommand {
        publicationId = requireText(publicationId, "publicationId");
        templateId = requireText(templateId, "templateId");
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Objects.requireNonNull(clientType, "clientType must not be null");
        Objects.requireNonNull(audience, "audience must not be null");
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
