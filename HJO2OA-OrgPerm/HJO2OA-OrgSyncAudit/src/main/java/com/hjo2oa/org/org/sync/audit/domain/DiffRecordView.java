package com.hjo2oa.org.org.sync.audit.domain;

import java.time.Instant;
import java.util.UUID;

public record DiffRecordView(
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
}
