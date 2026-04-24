package com.hjo2oa.data.data.sync.domain;

import java.util.Optional;
import java.util.UUID;

public interface SyncExecutionRecordRepository {

    Optional<SyncExecutionRecord> findById(UUID executionId);

    Optional<SyncExecutionRecord> findByTaskIdAndIdempotencyKey(UUID taskId, String idempotencyKey);

    Optional<SyncExecutionRecord> findLatestByTaskId(UUID taskId);

    Optional<SyncExecutionRecord> findLatestSuccessfulByTaskId(UUID taskId);

    PagedResult<SyncExecutionRecord> page(SyncExecutionFilter filter);

    void save(SyncExecutionRecord executionRecord);
}
