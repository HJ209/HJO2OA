package com.hjo2oa.org.org.sync.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ConflictRecord(
        UUID id,
        UUID tenantId,
        UUID diffRecordId,
        String conflictField,
        String sourceValue,
        String localValue,
        ConflictSeverity severity,
        Instant createdAt
) {

    public ConflictRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(diffRecordId, "diffRecordId must not be null");
        conflictField = SyncSourceConfig.requireText(conflictField, "conflictField");
        sourceValue = SyncSourceConfig.normalizeNullable(sourceValue);
        localValue = SyncSourceConfig.normalizeNullable(localValue);
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
