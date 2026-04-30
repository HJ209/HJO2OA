package com.hjo2oa.infra.scheduler.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hjo2oa.infra.scheduler.domain.ExecutionStatus;
import com.hjo2oa.infra.scheduler.domain.JobExecutionRecord;
import com.hjo2oa.infra.scheduler.domain.JobExecutionRecordRepository;
import com.hjo2oa.infra.scheduler.infrastructure.persistence.JobExecutionRecordEntity;
import com.hjo2oa.infra.scheduler.infrastructure.persistence.JobExecutionRecordMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class MybatisJobExecutionRecordRepository implements JobExecutionRecordRepository {

    private final JobExecutionRecordMapper jobExecutionRecordMapper;

    public MybatisJobExecutionRecordRepository(JobExecutionRecordMapper jobExecutionRecordMapper) {
        this.jobExecutionRecordMapper = jobExecutionRecordMapper;
    }

    @Override
    public Optional<JobExecutionRecord> findById(UUID id) {
        return Optional.ofNullable(jobExecutionRecordMapper.selectById(id.toString())).map(this::toDomain);
    }

    @Override
    public List<JobExecutionRecord> findRunningByScheduledJobId(UUID scheduledJobId) {
        LambdaQueryWrapper<JobExecutionRecordEntity> query = new LambdaQueryWrapper<JobExecutionRecordEntity>()
                .eq(JobExecutionRecordEntity::getScheduledJobId, scheduledJobId.toString())
                .eq(JobExecutionRecordEntity::getExecutionStatus, ExecutionStatus.RUNNING)
                .orderByAsc(JobExecutionRecordEntity::getStartedAt);
        return jobExecutionRecordMapper.selectList(query).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<JobExecutionRecord> findByJobIdAndIdempotencyKey(UUID scheduledJobId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        LambdaQueryWrapper<JobExecutionRecordEntity> query = new LambdaQueryWrapper<JobExecutionRecordEntity>()
                .eq(JobExecutionRecordEntity::getScheduledJobId, scheduledJobId.toString())
                .eq(JobExecutionRecordEntity::getIdempotencyKey, idempotencyKey);
        return jobExecutionRecordMapper.selectList(query).stream().findFirst().map(this::toDomain);
    }

    @Override
    public List<JobExecutionRecord> findByCriteria(UUID jobId, Instant from, Instant to) {
        return findByCriteria(jobId, null, from, to);
    }

    @Override
    public List<JobExecutionRecord> findByCriteria(
            UUID jobId,
            ExecutionStatus executionStatus,
            Instant from,
            Instant to
    ) {
        LambdaQueryWrapper<JobExecutionRecordEntity> query = new LambdaQueryWrapper<>();
        if (jobId != null) {
            query.eq(JobExecutionRecordEntity::getScheduledJobId, jobId.toString());
        }
        if (executionStatus != null) {
            query.eq(JobExecutionRecordEntity::getExecutionStatus, executionStatus);
        }
        if (from != null) {
            query.ge(JobExecutionRecordEntity::getStartedAt, from);
        }
        if (to != null) {
            query.le(JobExecutionRecordEntity::getStartedAt, to);
        }
        query.orderByDesc(JobExecutionRecordEntity::getStartedAt);
        return jobExecutionRecordMapper.selectList(query).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public JobExecutionRecord save(JobExecutionRecord executionRecord) {
        JobExecutionRecordEntity entity = toEntity(executionRecord);
        if (jobExecutionRecordMapper.selectById(entity.getId()) == null) {
            jobExecutionRecordMapper.insert(entity);
        } else {
            jobExecutionRecordMapper.updateById(entity);
        }
        return executionRecord;
    }

    private JobExecutionRecord toDomain(JobExecutionRecordEntity entity) {
        return new JobExecutionRecord(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getScheduledJobId()),
                entity.getParentExecutionId() == null ? null : UUID.fromString(entity.getParentExecutionId()),
                entity.getTriggerSource(),
                entity.getExecutionStatus(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getDurationMs(),
                entity.getAttemptNo(),
                entity.getMaxAttempts(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getErrorStack(),
                entity.getExecutionLog(),
                entity.getTriggerContext(),
                entity.getIdempotencyKey(),
                entity.getNextRetryAt()
        );
    }

    private JobExecutionRecordEntity toEntity(JobExecutionRecord executionRecord) {
        JobExecutionRecordEntity entity = new JobExecutionRecordEntity();
        entity.setId(executionRecord.id().toString());
        entity.setScheduledJobId(executionRecord.scheduledJobId().toString());
        entity.setParentExecutionId(
                executionRecord.parentExecutionId() == null ? null : executionRecord.parentExecutionId().toString()
        );
        entity.setTriggerSource(executionRecord.triggerSource());
        entity.setExecutionStatus(executionRecord.executionStatus());
        entity.setStartedAt(executionRecord.startedAt());
        entity.setFinishedAt(executionRecord.finishedAt());
        entity.setDurationMs(executionRecord.durationMs());
        entity.setAttemptNo(executionRecord.attemptNo());
        entity.setMaxAttempts(executionRecord.maxAttempts());
        entity.setErrorCode(executionRecord.errorCode());
        entity.setErrorMessage(executionRecord.errorMessage());
        entity.setErrorStack(executionRecord.errorStack());
        entity.setExecutionLog(executionRecord.executionLog());
        entity.setTriggerContext(executionRecord.triggerContext());
        entity.setIdempotencyKey(executionRecord.idempotencyKey());
        entity.setNextRetryAt(executionRecord.nextRetryAt());
        return entity;
    }
}
