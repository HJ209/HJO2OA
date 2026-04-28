package com.hjo2oa.infra.security.infrastructure.jwt;

import java.time.Instant;
import java.util.List;

public record JwtClaims(
        String personId,
        String username,
        List<String> roles,
        String tenantId,
        Instant issuedAt,
        Instant expiresAt
) {

    public JwtClaims {
        roles = List.copyOf(roles == null ? List.of() : roles);
    }
}
