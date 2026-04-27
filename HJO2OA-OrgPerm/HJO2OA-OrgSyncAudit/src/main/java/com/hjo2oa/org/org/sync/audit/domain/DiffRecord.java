package com.hjo2oa.org.org.sync.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DiffRecord(
        UUID id,
        UUID tenantId,
        UUID taskId,
        String entityType,
        String entityKey,
        DiffType diffType,
        DiffStatus status,
        String sourceSnapshot,
        String localSnapshot,
        String suggestion,
        UUID resolvedBy,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public DiffRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(taskId, "taskId must not be null");
        entityType = SyncSourceConfig.requireText(entityType, "entityType");
        entityKey = SyncSourceConfig.requireText(entityKey, "entityKey");
        Objects.requireNonNull(diffType, "diffType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        sourceSnapshot = SyncSourceConfig.normalizeNullable(sourceSnapshot);
        localSnapshot = SyncSourceConfig.normalizeNullable(localSnapshot);
        suggestion = SyncSourceConfig.normalizeNullable(suggestion);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static DiffRecord create(
            UUID id,
            UUID tenantId,
            UUID taskId,
            String entityType,
            String entityKey,
            DiffType diffType,
            String sourceSnapshot,
            String localSnapshot,
            String suggestion,
            Instant now
    ) {
        return new DiffRecord(
                id,
                tenantId,
                taskId,
                entityType,
                entityKey,
                diffType,
                DiffStatus.PENDING,
                sourceSnapshot,
                localSnapshot,
                suggestion,
                null,
                null,
                now,
                now
        );
    }

    public DiffRecord resolve(UUID operatorId, Instant now) {
        return withStatus(DiffStatus.RESOLVED, operatorId, now);
    }

    public DiffRecord ignore(UUID operatorId, Instant now) {
        return withStatus(DiffStatus.IGNORED, operatorId, now);
    }

    public DiffRecord confirm(UUID operatorId, Instant now) {
        return withStatus(DiffStatus.CONFIRMED, operatorId, now);
    }

    public DiffRecordView toView() {
        return new DiffRecordView(
                id,
                tenantId,
                taskId,
                entityType,
                entityKey,
                diffType,
                status,
                sourceSnapshot,
                localSnapshot,
                suggestion,
                resolvedBy,
                resolvedAt,
                createdAt,
                updatedAt
        );
    }

    private DiffRecord withStatus(DiffStatus nextStatus, UUID operatorId, Instant now) {
        return new DiffRecord(
                id,
                tenantId,
                taskId,
                entityType,
                entityKey,
                diffType,
                nextStatus,
                sourceSnapshot,
                localSnapshot,
                suggestion,
                operatorId,
                now,
                createdAt,
                now
        );
    }
}
