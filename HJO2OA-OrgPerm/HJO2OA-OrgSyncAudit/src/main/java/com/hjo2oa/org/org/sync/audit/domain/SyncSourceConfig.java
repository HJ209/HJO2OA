package com.hjo2oa.org.org.sync.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SyncSourceConfig(
        UUID id,
        UUID tenantId,
        String sourceCode,
        String sourceName,
        String sourceType,
        String endpoint,
        String configRef,
        String scopeExpression,
        SourceStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public SyncSourceConfig {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        sourceCode = requireText(sourceCode, "sourceCode");
        sourceName = requireText(sourceName, "sourceName");
        sourceType = requireText(sourceType, "sourceType");
        endpoint = normalizeNullable(endpoint);
        configRef = requireText(configRef, "configRef");
        scopeExpression = normalizeNullable(scopeExpression);
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static SyncSourceConfig create(
            UUID id,
            UUID tenantId,
            String sourceCode,
            String sourceName,
            String sourceType,
            String endpoint,
            String configRef,
            String scopeExpression,
            Instant now
    ) {
        return new SyncSourceConfig(
                id,
                tenantId,
                sourceCode,
                sourceName,
                sourceType,
                endpoint,
                configRef,
                scopeExpression,
                SourceStatus.DISABLED,
                now,
                now
        );
    }

    public SyncSourceConfig update(
            String sourceName,
            String sourceType,
            String endpoint,
            String configRef,
            String scopeExpression,
            Instant now
    ) {
        return new SyncSourceConfig(
                id,
                tenantId,
                sourceCode,
                sourceName,
                sourceType,
                endpoint,
                configRef,
                scopeExpression,
                status,
                createdAt,
                now
        );
    }

    public SyncSourceConfig enable(Instant now) {
        return withStatus(SourceStatus.ENABLED, now);
    }

    public SyncSourceConfig disable(Instant now) {
        return withStatus(SourceStatus.DISABLED, now);
    }

    public SyncSourceConfigView toView() {
        return new SyncSourceConfigView(
                id,
                tenantId,
                sourceCode,
                sourceName,
                sourceType,
                endpoint,
                configRef,
                scopeExpression,
                status,
                createdAt,
                updatedAt
        );
    }

    private SyncSourceConfig withStatus(SourceStatus nextStatus, Instant now) {
        if (status == nextStatus) {
            return this;
        }
        return new SyncSourceConfig(
                id,
                tenantId,
                sourceCode,
                sourceName,
                sourceType,
                endpoint,
                configRef,
                scopeExpression,
                nextStatus,
                createdAt,
                now
        );
    }

    static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
