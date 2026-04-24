package com.hjo2oa.infra.timezone.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

public record ResolvedTimezoneView(
        UUID settingId,
        UUID tenantId,
        UUID personId,
        TimezoneScopeType scopeType,
        UUID scopeId,
        String timezoneId,
        boolean isDefault,
        Instant effectiveFrom
) {

    private static final String UTC_TIMEZONE_ID = "UTC";

    public ResolvedTimezoneView {
        Objects.requireNonNull(scopeType, "scopeType must not be null");
        timezoneId = requireTimezoneId(timezoneId);
    }

    public static ResolvedTimezoneView from(TimezoneSetting setting, UUID tenantId, UUID personId) {
        Objects.requireNonNull(setting, "setting must not be null");
        return new ResolvedTimezoneView(
                setting.id(),
                tenantId,
                personId,
                setting.scopeType(),
                setting.scopeId(),
                setting.timezoneId(),
                setting.isDefault(),
                setting.effectiveFrom()
        );
    }

    public static ResolvedTimezoneView fallbackUtc(UUID tenantId, UUID personId) {
        return new ResolvedTimezoneView(
                null,
                tenantId,
                personId,
                TimezoneScopeType.SYSTEM,
                null,
                UTC_TIMEZONE_ID,
                true,
                null
        );
    }

    private static String requireTimezoneId(String value) {
        Objects.requireNonNull(value, "timezoneId must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("timezoneId must not be blank");
        }
        ZoneId.of(normalized);
        return normalized;
    }
}
