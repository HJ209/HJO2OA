package com.hjo2oa.org.org.sync.audit.domain;

import java.time.Instant;
import java.util.UUID;

public record AuditRecordView(
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
}
