package com.hjo2oa.org.org.sync.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuditRecord(
        UUID id,
        UUID tenantId,
        AuditCategory category,
        String actionType,
        String entityType,
        String entityId,
        UUID taskId,
        String triggerSource,
        UUID operatorId,
        String beforeSnapshot,
        String afterSnapshot,
        String summary,
        Instant occurredAt,
        Instant createdAt
) {

    public AuditRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(category, "category must not be null");
        actionType = SyncSourceConfig.requireText(actionType, "actionType");
        entityType = SyncSourceConfig.requireText(entityType, "entityType");
        entityId = SyncSourceConfig.normalizeNullable(entityId);
        triggerSource = SyncSourceConfig.requireText(triggerSource, "triggerSource");
        beforeSnapshot = SyncSourceConfig.normalizeNullable(beforeSnapshot);
        afterSnapshot = SyncSourceConfig.normalizeNullable(afterSnapshot);
        summary = SyncSourceConfig.normalizeNullable(summary);
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static AuditRecord create(
            UUID id,
            UUID tenantId,
            AuditCategory category,
            String actionType,
            String entityType,
            String entityId,
            UUID taskId,
            String triggerSource,
            UUID operatorId,
            String beforeSnapshot,
            String afterSnapshot,
            String summary,
            Instant now
    ) {
        return new AuditRecord(
                id,
                tenantId,
                category,
                actionType,
                entityType,
                entityId,
                taskId,
                triggerSource,
                operatorId,
                beforeSnapshot,
                afterSnapshot,
                summary,
                now,
                now
        );
    }

    public AuditRecordView toView() {
        return new AuditRecordView(
                id,
                tenantId,
                category,
                actionType,
                entityType,
                entityId,
                taskId,
                triggerSource,
                operatorId,
                beforeSnapshot,
                afterSnapshot,
                summary,
                occurredAt,
                createdAt
        );
    }
}
