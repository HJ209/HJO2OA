package com.hjo2oa.data.connector.domain;

import java.util.Objects;

public record ConnectorParameterTemplate(
        String paramKey,
        String displayName,
        ConnectorParameterTemplateCategory category,
        boolean required,
        boolean sensitive,
        String description
) {

    public ConnectorParameterTemplate {
        paramKey = requireText(paramKey, "paramKey");
        displayName = requireText(displayName, "displayName");
        Objects.requireNonNull(category, "category must not be null");
        description = description == null ? null : description.trim();
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
