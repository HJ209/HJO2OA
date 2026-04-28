package com.hjo2oa.org.org.sync.audit.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.org.org.sync.audit.domain.DiffRecord;
import com.hjo2oa.org.org.sync.audit.domain.DiffRecordRepository;
import com.hjo2oa.org.org.sync.audit.domain.DiffStatus;
import com.hjo2oa.org.org.sync.audit.domain.DiffType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisDiffRecordRepository implements DiffRecordRepository {

    private final DiffRecordMapper mapper;

    public MybatisDiffRecordRepository(DiffRecordMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public DiffRecord save(DiffRecord diffRecord) {
        DiffRecordEntity entity = toEntity(diffRecord);
        if (mapper.selectById(diffRecord.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findById(diffRecord.id()).orElseThrow();
    }

    @Override
    public Optional<DiffRecord> findById(UUID diffRecordId) {
        return Optional.ofNullable(mapper.selectById(diffRecordId)).map(this::toDomain);
    }

    @Override
    public List<DiffRecord> findByQuery(UUID tenantId, UUID taskId, DiffStatus status) {
        LambdaQueryWrapper<DiffRecordEntity> wrapper = Wrappers.<DiffRecordEntity>lambdaQuery()
                .eq(DiffRecordEntity::getTenantId, tenantId)
                .orderByDesc(DiffRecordEntity::getCreatedAt);
        if (taskId != null) {
            wrapper.eq(DiffRecordEntity::getTaskId, taskId);
        }
        if (status != null) {
            wrapper.eq(DiffRecordEntity::getStatus, status.name());
        }
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    private DiffRecord toDomain(DiffRecordEntity entity) {
        return new DiffRecord(
                entity.getId(),
                entity.getTenantId(),
                entity.getTaskId(),
                entity.getEntityType(),
                entity.getEntityKey(),
                DiffType.valueOf(entity.getDiffType()),
                DiffStatus.valueOf(entity.getStatus()),
                entity.getSourceSnapshot(),
                entity.getLocalSnapshot(),
                entity.getSuggestion(),
                entity.getResolvedBy(),
                entity.getResolvedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private DiffRecordEntity toEntity(DiffRecord diffRecord) {
        DiffRecordEntity entity = new DiffRecordEntity();
        entity.setId(diffRecord.id());
        entity.setTenantId(diffRecord.tenantId());
        entity.setTaskId(diffRecord.taskId());
        entity.setEntityType(diffRecord.entityType());
        entity.setEntityKey(diffRecord.entityKey());
        entity.setDiffType(diffRecord.diffType().name());
        entity.setStatus(diffRecord.status().name());
        entity.setSourceSnapshot(diffRecord.sourceSnapshot());
        entity.setLocalSnapshot(diffRecord.localSnapshot());
        entity.setSuggestion(diffRecord.suggestion());
        entity.setResolvedBy(diffRecord.resolvedBy());
        entity.setResolvedAt(diffRecord.resolvedAt());
        entity.setCreatedAt(diffRecord.createdAt());
        entity.setUpdatedAt(diffRecord.updatedAt());
        return entity;
    }
}
