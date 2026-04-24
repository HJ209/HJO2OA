package com.hjo2oa.data.openapi.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ApiCredentialGrant(
        String grantId,
        String openApiId,
        String tenantId,
        String clientCode,
        String secretRef,
        List<String> scopes,
        Instant expiresAt,
        ApiCredentialStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public ApiCredentialGrant {
        grantId = requireText(grantId, "grantId");
        openApiId = requireText(openApiId, "openApiId");
        tenantId = requireText(tenantId, "tenantId");
        clientCode = requireText(clientCode, "clientCode");
        secretRef = requireText(secretRef, "secretRef");
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ApiCredentialGrant create(
            String openApiId,
            String tenantId,
            String clientCode,
            String secretRef,
            List<String> scopes,
            Instant expiresAt,
            Instant now
    ) {
        return new ApiCredentialGrant(
                UUID.randomUUID().toString(),
                openApiId,
                tenantId,
                clientCode,
                secretRef,
                scopes,
                expiresAt,
                ApiCredentialStatus.ACTIVE,
                now,
                now
        );
    }

    public ApiCredentialGrant update(String secretRef, List<String> scopes, Instant expiresAt, Instant now) {
        return new ApiCredentialGrant(
                grantId,
                openApiId,
                tenantId,
                clientCode,
                secretRef,
                scopes,
                expiresAt,
                ApiCredentialStatus.ACTIVE,
                createdAt,
                now
        );
    }

    public ApiCredentialGrant revoke(Instant now) {
        return new ApiCredentialGrant(
                grantId,
                openApiId,
                tenantId,
                clientCode,
                secretRef,
                scopes,
                expiresAt,
                ApiCredentialStatus.REVOKED,
                createdAt,
                now
        );
    }

    public ApiCredentialStatus effectiveStatus(Instant now) {
        if (status == ApiCredentialStatus.REVOKED) {
            return ApiCredentialStatus.REVOKED;
        }
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            return ApiCredentialStatus.EXPIRED;
        }
        return ApiCredentialStatus.ACTIVE;
    }

    public boolean isActiveAt(Instant now) {
        return effectiveStatus(now) == ApiCredentialStatus.ACTIVE;
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
