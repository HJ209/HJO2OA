package com.hjo2oa.infra.tenant.domain;

import java.time.Instant;
import java.util.UUID;

public record TenantProfileView(
        UUID id,
        String tenantCode,
        String name,
        TenantStatus status,
        IsolationMode isolationMode,
        String packageCode,
        String defaultLocale,
        String defaultTimezone,
        boolean initialized,
        Instant createdAt,
        Instant updatedAt
) {
}
