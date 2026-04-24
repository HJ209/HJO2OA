package com.hjo2oa.infra.security.domain;

import java.time.Instant;
import java.util.UUID;

public record SecretKeyMaterialView(
        UUID id,
        UUID securityPolicyId,
        String keyRef,
        String algorithm,
        KeyStatus keyStatus,
        Instant rotatedAt
) {
}
