package com.hjo2oa.infra.security.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SecretKeyMaterial(
        UUID id,
        UUID securityPolicyId,
        String keyRef,
        String algorithm,
        KeyStatus keyStatus,
        Instant rotatedAt
) {

    public SecretKeyMaterial {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(securityPolicyId, "securityPolicyId must not be null");
        keyRef = requireText(keyRef, "keyRef");
        algorithm = requireText(algorithm, "algorithm");
        Objects.requireNonNull(keyStatus, "keyStatus must not be null");
    }

    public static SecretKeyMaterial create(UUID id, UUID securityPolicyId, String keyRef, String algorithm) {
        return new SecretKeyMaterial(id, securityPolicyId, keyRef, algorithm, KeyStatus.ACTIVE, null);
    }

    public SecretKeyMaterial activate(Instant rotatedAt) {
        return new SecretKeyMaterial(id, securityPolicyId, keyRef, algorithm, KeyStatus.ACTIVE, rotatedAt);
    }

    public SecretKeyMaterial markRotating(Instant rotatedAt) {
        return new SecretKeyMaterial(id, securityPolicyId, keyRef, algorithm, KeyStatus.ROTATING, rotatedAt);
    }

    public SecretKeyMaterial revoke(Instant rotatedAt) {
        return new SecretKeyMaterial(id, securityPolicyId, keyRef, algorithm, KeyStatus.REVOKED, rotatedAt);
    }

    public SecretKeyMaterialView toView() {
        return new SecretKeyMaterialView(id, securityPolicyId, keyRef, algorithm, keyStatus, rotatedAt);
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
