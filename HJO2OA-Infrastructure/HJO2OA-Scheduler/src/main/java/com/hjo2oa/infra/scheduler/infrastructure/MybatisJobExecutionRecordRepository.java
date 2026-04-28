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
    public List<JobExecutionRecord> findByCriteria(UUID jobId, Instant from, Instant to) {
        LambdaQueryWrapper<JobExecutionRecordEntity> query = new LambdaQueryWrapper<>();
        if (jobId != null) {
            query.eq(JobExecutionRecordEntity::getScheduledJobId, jobId.toString());
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
                entity.getTriggerSource(),
                entity.getExecutionStatus(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getExecutionLog()
        );
    }

    private JobExecutionRecordEntity toEntity(JobExecutionRecord executionRecord) {
        JobExecutionRecordEntity entity = new JobExecutionRecordEntity();
        entity.setId(executionRecord.id().toString());
        entity.setScheduledJobId(executionRecord.scheduledJobId().toString());
        entity.setTriggerSource(executionRecord.triggerSource());
        entity.setExecutionStatus(executionRecord.executionStatus());
        entity.setStartedAt(executionRecord.startedAt());
        entity.setFinishedAt(executionRecord.finishedAt());
        entity.setErrorCode(executionRecord.errorCode());
        entity.setErrorMessage(executionRecord.errorMessage());
        entity.setExecutionLog(executionRecord.executionLog());
        return entity;
    }
}
