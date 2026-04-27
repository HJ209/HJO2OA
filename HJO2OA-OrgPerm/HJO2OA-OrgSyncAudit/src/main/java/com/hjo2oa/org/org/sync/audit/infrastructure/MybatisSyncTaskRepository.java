package com.hjo2oa.org.org.sync.audit.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.org.org.sync.audit.domain.SyncTask;
import com.hjo2oa.org.org.sync.audit.domain.SyncTaskRepository;
import com.hjo2oa.org.org.sync.audit.domain.SyncTaskStatus;
import com.hjo2oa.org.org.sync.audit.domain.SyncTaskType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisSyncTaskRepository implements SyncTaskRepository {

    private final SyncTaskMapper mapper;

    public MybatisSyncTaskRepository(SyncTaskMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public SyncTask save(SyncTask task) {
        SyncTaskEntity entity = toEntity(task);
        if (mapper.selectById(task.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findById(task.id()).orElseThrow();
    }

    @Override
    public Optional<SyncTask> findById(UUID taskId) {
        return Optional.ofNullable(mapper.selectById(taskId)).map(this::toDomain);
    }

    @Override
    public boolean existsActiveTask(UUID tenantId, UUID sourceId) {
        LambdaQueryWrapper<SyncTaskEntity> wrapper = Wrappers.<SyncTaskEntity>lambdaQuery()
                .eq(SyncTaskEntity::getTenantId, tenantId)
                .eq(SyncTaskEntity::getSourceId, sourceId)
                .in(SyncTaskEntity::getStatus, SyncTaskStatus.CREATED.name(), SyncTaskStatus.RUNNING.name());
        return mapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<SyncTask> findByTenantId(UUID tenantId) {
        LambdaQueryWrapper<SyncTaskEntity> wrapper = Wrappers.<SyncTaskEntity>lambdaQuery()
                .eq(SyncTaskEntity::getTenantId, tenantId)
                .orderByDesc(SyncTaskEntity::getCreatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public List<SyncTask> findByTenantIdAndSourceId(UUID tenantId, UUID sourceId) {
        LambdaQueryWrapper<SyncTaskEntity> wrapper = Wrappers.<SyncTaskEntity>lambdaQuery()
                .eq(SyncTaskEntity::getTenantId, tenantId)
                .eq(SyncTaskEntity::getSourceId, sourceId)
                .orderByDesc(SyncTaskEntity::getCreatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    private SyncTask toDomain(SyncTaskEntity entity) {
        return new SyncTask(
                entity.getId(),
                entity.getTenantId(),
                entity.getSourceId(),
                SyncTaskType.valueOf(entity.getTaskType()),
                SyncTaskStatus.valueOf(entity.getStatus()),
                entity.getRetryOfTaskId(),
                entity.getTriggerSource(),
                entity.getOperatorId(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getSuccessCount() == null ? 0 : entity.getSuccessCount(),
                entity.getFailureCount() == null ? 0 : entity.getFailureCount(),
                entity.getDiffCount() == null ? 0 : entity.getDiffCount(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private SyncTaskEntity toEntity(SyncTask task) {
        SyncTaskEntity entity = new SyncTaskEntity();
        entity.setId(task.id());
        entity.setTenantId(task.tenantId());
        entity.setSourceId(task.sourceId());
        entity.setTaskType(task.taskType().name());
        entity.setStatus(task.status().name());
        entity.setRetryOfTaskId(task.retryOfTaskId());
        entity.setTriggerSource(task.triggerSource());
        entity.setOperatorId(task.operatorId());
        entity.setStartedAt(task.startedAt());
        entity.setFinishedAt(task.finishedAt());
        entity.setSuccessCount(task.successCount());
        entity.setFailureCount(task.failureCount());
        entity.setDiffCount(task.diffCount());
        entity.setFailureReason(task.failureReason());
        entity.setCreatedAt(task.createdAt());
        entity.setUpdatedAt(task.updatedAt());
        return entity;
    }
}
