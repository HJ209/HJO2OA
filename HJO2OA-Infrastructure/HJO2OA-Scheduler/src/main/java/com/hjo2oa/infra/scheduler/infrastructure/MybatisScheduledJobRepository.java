package com.hjo2oa.infra.scheduler.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hjo2oa.infra.scheduler.domain.ScheduledJob;
import com.hjo2oa.infra.scheduler.domain.ScheduledJobRepository;
import com.hjo2oa.infra.scheduler.infrastructure.persistence.ScheduledJobEntity;
import com.hjo2oa.infra.scheduler.infrastructure.persistence.ScheduledJobMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class MybatisScheduledJobRepository implements ScheduledJobRepository {

    private final ScheduledJobMapper scheduledJobMapper;

    public MybatisScheduledJobRepository(ScheduledJobMapper scheduledJobMapper) {
        this.scheduledJobMapper = scheduledJobMapper;
    }

    @Override
    public Optional<ScheduledJob> findById(UUID id) {
        return Optional.ofNullable(scheduledJobMapper.selectById(id.toString())).map(this::toDomain);
    }

    @Override
    public Optional<ScheduledJob> findByJobCode(String jobCode) {
        LambdaQueryWrapper<ScheduledJobEntity> query = new LambdaQueryWrapper<ScheduledJobEntity>()
                .eq(ScheduledJobEntity::getJobCode, jobCode);
        return scheduledJobMapper.selectList(query).stream().findFirst().map(this::toDomain);
    }

    @Override
    public List<ScheduledJob> findAll() {
        return scheduledJobMapper.selectAllForRuntime().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ScheduledJob> findByTenantId(UUID tenantId) {
        LambdaQueryWrapper<ScheduledJobEntity> query = new LambdaQueryWrapper<ScheduledJobEntity>()
                .eq(ScheduledJobEntity::getTenantId, tenantId.toString());
        return scheduledJobMapper.selectList(query).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public ScheduledJob save(ScheduledJob scheduledJob) {
        ScheduledJobEntity entity = toEntity(scheduledJob);
        if (scheduledJobMapper.selectById(entity.getId()) == null) {
            scheduledJobMapper.insert(entity);
        } else {
            scheduledJobMapper.updateById(entity);
        }
        return scheduledJob;
    }

    private ScheduledJob toDomain(ScheduledJobEntity entity) {
        return new ScheduledJob(
                UUID.fromString(entity.getId()),
                entity.getJobCode(),
                entity.getHandlerName(),
                entity.getName(),
                entity.getTriggerType(),
                entity.getCronExpr(),
                entity.getTimezoneId(),
                entity.getConcurrencyPolicy(),
                entity.getTimeoutSeconds(),
                entity.getRetryPolicy(),
                entity.getStatus(),
                entity.getTenantId() == null ? null : UUID.fromString(entity.getTenantId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ScheduledJobEntity toEntity(ScheduledJob scheduledJob) {
        ScheduledJobEntity entity = new ScheduledJobEntity();
        entity.setId(scheduledJob.id().toString());
        entity.setJobCode(scheduledJob.jobCode());
        entity.setHandlerName(scheduledJob.handlerName());
        entity.setName(scheduledJob.name());
        entity.setTriggerType(scheduledJob.triggerType());
        entity.setCronExpr(scheduledJob.cronExpr());
        entity.setTimezoneId(scheduledJob.timezoneId());
        entity.setConcurrencyPolicy(scheduledJob.concurrencyPolicy());
        entity.setTimeoutSeconds(scheduledJob.timeoutSeconds());
        entity.setRetryPolicy(scheduledJob.retryPolicy());
        entity.setStatus(scheduledJob.status());
        entity.setTenantId(scheduledJob.tenantId() == null ? null : scheduledJob.tenantId().toString());
        entity.setCreatedAt(scheduledJob.createdAt());
        entity.setUpdatedAt(scheduledJob.updatedAt());
        return entity;
    }
}
