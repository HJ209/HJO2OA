package com.hjo2oa.infra.timezone.domain;

import java.time.Instant;
import java.util.UUID;

public record TimezoneSettingView(
        UUID id,
        TimezoneScopeType scopeType,
        UUID scopeId,
        String timezoneId,
        boolean isDefault,
        Instant effectiveFrom,
        boolean active,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
