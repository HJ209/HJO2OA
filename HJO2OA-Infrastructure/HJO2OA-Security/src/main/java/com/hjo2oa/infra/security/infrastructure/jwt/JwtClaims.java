package com.hjo2oa.infra.security.infrastructure.jwt;

import java.time.Instant;
import java.util.List;

public record JwtClaims(
        String personId,
        String username,
        List<String> roles,
        String tenantId,
        String accountId,
        String currentAssignmentId,
        String currentPositionId,
        String currentOrganizationId,
        String currentDepartmentId,
        long permissionSnapshotVersion,
        Instant issuedAt,
        Instant expiresAt
) {

    public JwtClaims(
            String personId,
            String username,
            List<String> roles,
            String tenantId,
            Instant issuedAt,
            Instant expiresAt
    ) {
        this(personId, username, roles, tenantId, null, null, null, null, null, 0L, issuedAt, expiresAt);
    }

    public JwtClaims {
        roles = List.copyOf(roles == null ? List.of() : roles);
    }
}
