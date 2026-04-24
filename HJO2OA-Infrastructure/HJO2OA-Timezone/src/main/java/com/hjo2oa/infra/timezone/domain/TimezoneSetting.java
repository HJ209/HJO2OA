package com.hjo2oa.infra.timezone.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

public record TimezoneSetting(
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

    public TimezoneSetting {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(scopeType, "scopeType must not be null");
        timezoneId = requireTimezoneId(timezoneId);
        validateScope(scopeType, scopeId, tenantId);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }

    public static TimezoneSetting create(
            TimezoneScopeType scopeType,
            UUID scopeId,
            String timezoneId,
            boolean isDefault,
            Instant effectiveFrom,
            UUID tenantId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");
        return new TimezoneSetting(
                UUID.randomUUID(),
                scopeType,
                scopeId,
                timezoneId,
                isDefault,
                effectiveFrom,
                true,
                tenantId,
                now,
                now
        );
    }

    public TimezoneSetting update(
            String timezoneId,
            boolean isDefault,
            Instant effectiveFrom,
            boolean active,
            UUID tenantId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");
        return new TimezoneSetting(
                id,
                scopeType,
                scopeId,
                timezoneId,
                isDefault,
                effectiveFrom,
                active,
                tenantId,
                createdAt,
                now
        );
    }

    public TimezoneSetting deactivate(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new TimezoneSetting(
                id,
                scopeType,
                scopeId,
                timezoneId,
                isDefault,
                effectiveFrom,
                false,
                tenantId,
                createdAt,
                now
        );
    }

    public TimezoneSettingView toView() {
        return new TimezoneSettingView(
                id,
                scopeType,
                scopeId,
                timezoneId,
                isDefault,
                effectiveFrom,
                active,
                tenantId,
                createdAt,
                updatedAt
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

    private static void validateScope(TimezoneScopeType scopeType, UUID scopeId, UUID tenantId) {
        if (scopeType == TimezoneScopeType.SYSTEM) {
            if (scopeId != null) {
                throw new IllegalArgumentException("scopeId must be null for system timezone setting");
            }
            if (tenantId != null) {
                throw new IllegalArgumentException("tenantId must be null for system timezone setting");
            }
            return;
        }
        Objects.requireNonNull(scopeId, "scopeId must not be null for tenant or person timezone setting");
        if (scopeType == TimezoneScopeType.TENANT) {
            Objects.requireNonNull(tenantId, "tenantId must not be null for tenant timezone setting");
            if (!tenantId.equals(scopeId)) {
                throw new IllegalArgumentException("tenant scopeId must equal tenantId");
            }
        }
    }
}
