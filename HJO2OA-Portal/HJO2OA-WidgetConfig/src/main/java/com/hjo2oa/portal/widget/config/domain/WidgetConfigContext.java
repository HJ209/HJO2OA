package com.hjo2oa.portal.widget.config.domain;

import java.util.Objects;

public record WidgetConfigContext(
        String tenantId,
        String operatorId
) {

    public WidgetConfigContext {
        tenantId = requireText(tenantId, "tenantId");
        operatorId = requireText(operatorId, "operatorId");
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
