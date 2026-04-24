package com.hjo2oa.infra.audit.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AuditRecord(
        UUID id,
        String moduleCode,
        String objectType,
        String objectId,
        String actionType,
        UUID operatorAccountId,
        UUID operatorPersonId,
        UUID tenantId,
        String traceId,
        String summary,
        Instant occurredAt,
        ArchiveStatus archiveStatus,
        Instant createdAt,
        List<AuditFieldChange> fieldChanges
) {

    public AuditRecord {
        Objects.requireNonNull(id, "id must not be null");
        moduleCode = requireText(moduleCode, "moduleCode");
        objectType = requireText(objectType, "objectType");
        objectId = requireText(objectId, "objectId");
        actionType = requireText(actionType, "actionType");
        traceId = normalizeNullable(traceId);
        summary = normalizeNullable(summary);
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(archiveStatus, "archiveStatus must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        fieldChanges = normalizeFieldChanges(id, fieldChanges);
    }

    public static AuditRecord create(
            UUID id,
            String moduleCode,
            String objectType,
            String objectId,
            String actionType,
            UUID operatorAccountId,
            UUID operatorPersonId,
            UUID tenantId,
            String traceId,
            String summary,
            Instant occurredAt,
            Instant createdAt,
            List<AuditFieldChange> fieldChanges
    ) {
        return new AuditRecord(
                id,
                moduleCode,
                objectType,
                objectId,
                actionType,
                operatorAccountId,
                operatorPersonId,
                tenantId,
                traceId,
                summary,
                occurredAt,
                ArchiveStatus.ACTIVE,
                createdAt,
                fieldChanges
        );
    }

    public AuditRecord archive() {
        if (archiveStatus == ArchiveStatus.ARCHIVED) {
            return this;
        }
        return new AuditRecord(
                id,
                moduleCode,
                objectType,
                objectId,
                actionType,
                operatorAccountId,
                operatorPersonId,
                tenantId,
                traceId,
                summary,
                occurredAt,
                ArchiveStatus.ARCHIVED,
                createdAt,
                fieldChanges
        );
    }

    public AuditRecordView toView() {
        return new AuditRecordView(
                id,
                moduleCode,
                objectType,
                objectId,
                actionType,
                operatorAccountId,
                operatorPersonId,
                tenantId,
                traceId,
                summary,
                occurredAt,
                archiveStatus,
                createdAt,
                fieldChanges.stream().map(AuditFieldChange::toView).toList()
        );
    }

    private static List<AuditFieldChange> normalizeFieldChanges(UUID auditRecordId, List<AuditFieldChange> fieldChanges) {
        if (fieldChanges == null || fieldChanges.isEmpty()) {
            return List.of();
        }
        return fieldChanges.stream()
                .map(fieldChange -> new AuditFieldChange(
                        fieldChange.id(),
                        auditRecordId,
                        fieldChange.fieldName(),
                        fieldChange.oldValue(),
                        fieldChange.newValue(),
                        fieldChange.sensitivityLevel()
                ))
                .sorted(Comparator.comparing(AuditFieldChange::fieldName).thenComparing(AuditFieldChange::id))
                .toList();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
