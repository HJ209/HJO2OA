package com.hjo2oa.portal.portal.model.application;

import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import java.util.Objects;

public record CreatePortalTemplateCommand(
        String templateId,
        String templateCode,
        String displayName,
        PortalPublicationSceneType sceneType
) {

    public CreatePortalTemplateCommand {
        templateId = requireText(templateId, "templateId");
        templateCode = requireText(templateCode, "templateCode");
        displayName = requireText(displayName, "displayName");
        Objects.requireNonNull(sceneType, "sceneType must not be null");
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
