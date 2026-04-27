package com.hjo2oa.org.role.resource.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record PersonRoleView(
        UUID id,
        UUID personId,
        UUID roleId,
        String reason,
        Instant expiresAt,
        UUID tenantId
) {
}
