package com.hjo2oa.org.org.sync.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CompensationRecord(
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

    public CompensationRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(diffRecordId, "diffRecordId must not be null");
        actionType = SyncSourceConfig.requireText(actionType, "actionType");
        Objects.requireNonNull(status, "status must not be null");
        requestPayload = SyncSourceConfig.normalizeNullable(requestPayload);
        resultPayload = SyncSourceConfig.normalizeNullable(resultPayload);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static CompensationRecord request(
            UUID id,
            UUID tenantId,
            UUID taskId,
            UUID diffRecordId,
            String actionType,
            String requestPayload,
            UUID operatorId,
            Instant now
    ) {
        return new CompensationRecord(
                id,
                tenantId,
                taskId,
                diffRecordId,
                actionType,
                CompensationStatus.REQUESTED,
                requestPayload,
                null,
                operatorId,
                now,
                now
        );
    }

    public CompensationRecord applied(String resultPayload, Instant now) {
        return withStatus(CompensationStatus.APPLIED, resultPayload, now);
    }

    public CompensationRecord failed(String resultPayload, Instant now) {
        return withStatus(CompensationStatus.FAILED, resultPayload, now);
    }

    public CompensationRecordView toView() {
        return new CompensationRecordView(
                id,
                tenantId,
                taskId,
                diffRecordId,
                actionType,
                status,
                requestPayload,
                resultPayload,
                operatorId,
                createdAt,
                updatedAt
        );
    }

    private CompensationRecord withStatus(CompensationStatus nextStatus, String resultPayload, Instant now) {
        return new CompensationRecord(
                id,
                tenantId,
                taskId,
                diffRecordId,
                actionType,
                nextStatus,
                requestPayload,
                resultPayload,
                operatorId,
                createdAt,
                now
        );
    }
}
