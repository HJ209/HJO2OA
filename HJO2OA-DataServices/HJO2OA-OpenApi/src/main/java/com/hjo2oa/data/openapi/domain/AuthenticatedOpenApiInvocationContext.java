package com.hjo2oa.data.openapi.domain;

import java.time.Instant;
import java.util.Objects;

public record AuthenticatedOpenApiInvocationContext(
        String requestId,
        String tenantId,
        String clientCode,
        OpenApiEndpoint endpoint,
        ApiCredentialGrant credentialGrant,
        Instant invokedAt
) {

    public AuthenticatedOpenApiInvocationContext {
        requestId = requireText(requestId, "requestId");
        tenantId = requireText(tenantId, "tenantId");
        clientCode = requireText(clientCode, "clientCode");
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        Objects.requireNonNull(credentialGrant, "credentialGrant must not be null");
        Objects.requireNonNull(invokedAt, "invokedAt must not be null");
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
