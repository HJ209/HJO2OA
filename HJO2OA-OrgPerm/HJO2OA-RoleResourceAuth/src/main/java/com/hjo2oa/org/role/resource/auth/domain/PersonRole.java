package com.hjo2oa.org.role.resource.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PersonRole(
        UUID id,
        UUID personId,
        UUID roleId,
        String reason,
        Instant expiresAt,
        UUID tenantId
) {

    public PersonRole {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(personId, "personId must not be null");
        Objects.requireNonNull(roleId, "roleId must not be null");
        reason = Role.requireText(reason, "reason");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
    }

    public boolean expiredAt(Instant instant) {
        return expiresAt != null && !expiresAt.isAfter(instant);
    }

    public PersonRoleView toView() {
        return new PersonRoleView(id, personId, roleId, reason, expiresAt, tenantId);
    }
}
