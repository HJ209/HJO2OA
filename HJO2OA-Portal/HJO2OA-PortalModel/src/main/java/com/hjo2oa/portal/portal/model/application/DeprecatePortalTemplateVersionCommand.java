package com.hjo2oa.portal.portal.model.application;

import java.util.Objects;

public record DeprecatePortalTemplateVersionCommand(
        String templateId,
        int versionNo
) {

    public DeprecatePortalTemplateVersionCommand {
        templateId = requireText(templateId, "templateId");
        if (versionNo <= 0) {
            throw new IllegalArgumentException("versionNo must be greater than 0");
        }
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
