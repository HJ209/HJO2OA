package com.hjo2oa.infra.tenant.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TenantProfile(
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

    public TenantProfile {
        Objects.requireNonNull(id, "id must not be null");
        tenantCode = requireText(tenantCode, "tenantCode");
        name = requireText(name, "name");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(isolationMode, "isolationMode must not be null");
        packageCode = normalizeOptional(packageCode);
        defaultLocale = normalizeOptional(defaultLocale);
        defaultTimezone = normalizeOptional(defaultTimezone);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static TenantProfile create(
            UUID id,
            String tenantCode,
            String name,
            IsolationMode isolationMode,
            String packageCode,
            String defaultLocale,
            String defaultTimezone,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");
        return new TenantProfile(
                id,
                tenantCode,
                name,
                TenantStatus.DRAFT,
                isolationMode,
                packageCode,
                defaultLocale,
                defaultTimezone,
                false,
                now,
                now
        );
    }

    public TenantProfile activate(Instant now) {
        return changeStatus(TenantStatus.ACTIVE, now);
    }

    public TenantProfile suspend(Instant now) {
        return changeStatus(TenantStatus.SUSPENDED, now);
    }

    public TenantProfile archive(Instant now) {
        return changeStatus(TenantStatus.ARCHIVED, now);
    }

    public TenantProfile markInitialized(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (initialized) {
            return this;
        }
        return new TenantProfile(
                id,
                tenantCode,
                name,
                status,
                isolationMode,
                packageCode,
                defaultLocale,
                defaultTimezone,
                true,
                createdAt,
                now
        );
    }

    public TenantProfileView toView() {
        return new TenantProfileView(
                id,
                tenantCode,
                name,
                status,
                isolationMode,
                packageCode,
                defaultLocale,
                defaultTimezone,
                initialized,
                createdAt,
                updatedAt
        );
    }

    private TenantProfile changeStatus(TenantStatus nextStatus, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(nextStatus, "nextStatus must not be null");
        if (status == nextStatus) {
            return this;
        }
        return new TenantProfile(
                id,
                tenantCode,
                name,
                nextStatus,
                isolationMode,
                packageCode,
                defaultLocale,
                defaultTimezone,
                initialized,
                createdAt,
                now
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
