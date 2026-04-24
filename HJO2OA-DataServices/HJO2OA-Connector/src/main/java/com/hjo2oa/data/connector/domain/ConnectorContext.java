package com.hjo2oa.data.connector.domain;

import java.util.Objects;

public record ConnectorContext(
        String tenantId,
        String operatorId,
        String environment
) {

    public ConnectorContext {
        tenantId = requireText(tenantId, "tenantId");
        operatorId = requireText(operatorId, "operatorId");
        environment = requireText(environment, "environment");
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
