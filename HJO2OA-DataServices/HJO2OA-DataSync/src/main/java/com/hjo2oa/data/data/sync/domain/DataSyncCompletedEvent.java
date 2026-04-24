package com.hjo2oa.data.data.sync.domain;

import com.hjo2oa.data.common.domain.event.AbstractDataDomainEvent;
import com.hjo2oa.data.common.domain.event.DataEventTypes;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DataSyncCompletedEvent extends AbstractDataDomainEvent {

    private final UUID taskId;
    private final UUID executionId;
    private final String code;
    private final SyncMode syncMode;
    private final String checkpoint;
    private final long insertedCount;
    private final long updatedCount;
    private final long failedCount;
    private final long successCount;
    private final Instant finishedAt;

    public DataSyncCompletedEvent(
            String tenantId,
            String operatorId,
            UUID taskId,
            UUID executionId,
            String code,
            SyncMode syncMode,
            String checkpoint,
            long insertedCount,
            long updatedCount,
            long failedCount,
            long successCount,
            Instant finishedAt
    ) {
        super(
                DataEventTypes.DATA_SYNC_COMPLETED,
                tenantId,
                "data-sync",
                code,
                operatorId,
                payloadOf(taskId, executionId, code, syncMode, checkpoint, insertedCount, updatedCount, failedCount, successCount, finishedAt)
        );
        this.taskId = taskId;
        this.executionId = executionId;
        this.code = code;
        this.syncMode = syncMode;
        this.checkpoint = checkpoint;
        this.insertedCount = insertedCount;
        this.updatedCount = updatedCount;
        this.failedCount = failedCount;
        this.successCount = successCount;
        this.finishedAt = finishedAt;
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

    public long insertedCount() {
        return insertedCount;
    }

    public long updatedCount() {
        return updatedCount;
    }

    public long failedCount() {
        return failedCount;
    }

    public long successCount() {
        return successCount;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    private static Map<String, Object> payloadOf(
            UUID taskId,
            UUID executionId,
            String code,
            SyncMode syncMode,
            String checkpoint,
            long insertedCount,
            long updatedCount,
            long failedCount,
            long successCount,
            Instant finishedAt
    ) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", String.valueOf(taskId));
        payload.put("executionId", String.valueOf(executionId));
        payload.put("code", code);
        payload.put("syncMode", String.valueOf(syncMode));
        payload.put("checkpoint", checkpoint);
        payload.put("insertedCount", insertedCount);
        payload.put("updatedCount", updatedCount);
        payload.put("failedCount", failedCount);
        payload.put("successCount", successCount);
        payload.put("finishedAt", finishedAt == null ? null : finishedAt.toString());
        return payload;
    }
}
