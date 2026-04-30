package com.hjo2oa.infra.scheduler.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobExecutionRecordRepository {

    Optional<JobExecutionRecord> findById(UUID id);

    List<JobExecutionRecord> findRunningByScheduledJobId(UUID scheduledJobId);

    Optional<JobExecutionRecord> findByJobIdAndIdempotencyKey(UUID scheduledJobId, String idempotencyKey);

    List<JobExecutionRecord> findByCriteria(UUID jobId, Instant from, Instant to);

    List<JobExecutionRecord> findByCriteria(UUID jobId, ExecutionStatus executionStatus, Instant from, Instant to);

    JobExecutionRecord save(JobExecutionRecord executionRecord);
}
