package com.hjo2oa.org.org.sync.audit.domain;

import java.time.Instant;
import java.util.UUID;

public record CompensationRecordView(
        UUID id,
        UUID tenantId,
        UUID taskId,
        UUID diffRecordId,
        String actionType,
        CompensationStatus status,
        String requestPayload,
        String resultPayload,
        UUID operatorId,
        Instant createdAt,
        Instant updatedAt
) {
}
