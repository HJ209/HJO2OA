package com.hjo2oa.infra.scheduler.infrastructure;

import com.hjo2oa.infra.scheduler.domain.ExecutionStatus;
import com.hjo2oa.infra.scheduler.domain.JobExecutionRecord;
import com.hjo2oa.infra.scheduler.domain.JobExecutionRecordRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryJobExecutionRecordRepository implements JobExecutionRecordRepository {

    private final Map<UUID, JobExecutionRecord> recordsById = new ConcurrentHashMap<>();

    @Override
    public Optional<JobExecutionRecord> findById(UUID id) {
        return Optional.ofNullable(recordsById.get(id));
    }

    @Override
    public List<JobExecutionRecord> findRunningByScheduledJobId(UUID scheduledJobId) {
        return recordsById.values().stream()
                .filter(record -> record.scheduledJobId().equals(scheduledJobId))
                .filter(record -> record.executionStatus() == ExecutionStatus.RUNNING)
                .sorted(Comparator.comparing(JobExecutionRecord::startedAt))
                .toList();
    }

    @Override
    public List<JobExecutionRecord> findByCriteria(UUID jobId, Instant from, Instant to) {
        return recordsById.values().stream()
                .filter(record -> jobId == null || record.scheduledJobId().equals(jobId))
                .filter(record -> from == null || !record.startedAt().isBefore(from))
                .filter(record -> to == null || !record.startedAt().isAfter(to))
                .sorted(Comparator.comparing(JobExecutionRecord::startedAt).reversed())
                .toList();
    }

    @Override
    public JobExecutionRecord save(JobExecutionRecord executionRecord) {
        recordsById.put(executionRecord.id(), executionRecord);
        return executionRecord;
    }
}
