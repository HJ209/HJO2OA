package com.hjo2oa.data.data.sync.domain;

import com.hjo2oa.data.common.domain.event.AbstractDataDomainEvent;
import com.hjo2oa.data.common.domain.event.DataEventTypes;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DataSyncFailedEvent extends AbstractDataDomainEvent {

    private final UUID taskId;
    private final UUID executionId;
    private final String code;
    private final SyncMode syncMode;
    private final String checkpoint;
    private final String errorCode;
    private final boolean retryable;
    private final Instant failedAt;

    public DataSyncFailedEvent(
            String tenantId,
            String operatorId,
            UUID taskId,
            UUID executionId,
            String code,
            SyncMode syncMode,
            String checkpoint,
            String errorCode,
            boolean retryable,
            Instant failedAt
    ) {
        super(
                DataEventTypes.DATA_SYNC_FAILED,
                tenantId,
                "data-sync",
                code,
                operatorId,
                payloadOf(taskId, executionId, code, syncMode, checkpoint, errorCode, retryable, failedAt)
        );
        this.taskId = taskId;
        this.executionId = executionId;
        this.code = code;
        this.syncMode = syncMode;
        this.checkpoint = checkpoint;
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.failedAt = failedAt;
    }

    public UUID taskId() {
        return taskId;
    }

    public UUID executionId() {
        return executionId;
    }

    public String code() {
        return code;
    }

    public SyncMode syncMode() {
        return syncMode;
    }

    public String checkpoint() {
        return checkpoint;
    }

    public String errorCode() {
        return errorCode;
    }

    public boolean retryable() {
        return retryable;
    }

    public Instant failedAt() {
        return failedAt;
    }

    private static Map<String, Object> payloadOf(
            UUID taskId,
            UUID executionId,
            String code,
            SyncMode syncMode,
            String checkpoint,
            String errorCode,
            boolean retryable,
            Instant failedAt
    ) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", String.valueOf(taskId));
        payload.put("executionId", String.valueOf(executionId));
        payload.put("code", code);
        payload.put("syncMode", String.valueOf(syncMode));
        payload.put("checkpoint", checkpoint);
        payload.put("errorCode", errorCode);
        payload.put("retryable", retryable);
        payload.put("failedAt", failedAt == null ? null : failedAt.toString());
        return payload;
    }
}
