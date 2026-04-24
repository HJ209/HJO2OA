package com.hjo2oa.infra.scheduler.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobExecutionRecordRepository {

    Optional<JobExecutionRecord> findById(UUID id);

    List<JobExecutionRecord> findRunningByScheduledJobId(UUID scheduledJobId);

    List<JobExecutionRecord> findByCriteria(UUID jobId, Instant from, Instant to);

    JobExecutionRecord save(JobExecutionRecord executionRecord);
}
