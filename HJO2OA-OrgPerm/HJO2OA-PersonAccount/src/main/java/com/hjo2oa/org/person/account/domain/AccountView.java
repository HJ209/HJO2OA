package com.hjo2oa.org.person.account.domain;

import java.time.Instant;
import java.util.UUID;

public record AccountView(
        UUID id,
        UUID personId,
        String username,
        AccountType accountType,
        boolean primaryAccount,
        boolean locked,
        Instant lockedUntil,
        Instant lastLoginAt,
        String lastLoginIp,
        Instant passwordChangedAt,
        boolean mustChangePassword,
        AccountStatus status,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
