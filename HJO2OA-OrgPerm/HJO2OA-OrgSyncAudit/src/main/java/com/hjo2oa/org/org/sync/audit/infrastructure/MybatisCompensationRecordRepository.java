package com.hjo2oa.org.org.sync.audit.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.org.org.sync.audit.domain.CompensationRecord;
import com.hjo2oa.org.org.sync.audit.domain.CompensationRecordRepository;
import com.hjo2oa.org.org.sync.audit.domain.CompensationStatus;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisCompensationRecordRepository implements CompensationRecordRepository {

    private final CompensationRecordMapper mapper;

    public MybatisCompensationRecordRepository(CompensationRecordMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public CompensationRecord save(CompensationRecord compensationRecord) {
        CompensationRecordEntity entity = toEntity(compensationRecord);
        if (mapper.selectById(compensationRecord.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findById(compensationRecord.id()).orElseThrow();
    }

    @Override
    public Optional<CompensationRecord> findById(UUID compensationRecordId) {
        return Optional.ofNullable(mapper.selectById(compensationRecordId)).map(this::toDomain);
    }

    @Override
    public List<CompensationRecord> findByTenantIdAndTaskId(UUID tenantId, UUID taskId) {
        LambdaQueryWrapper<CompensationRecordEntity> wrapper = Wrappers.<CompensationRecordEntity>lambdaQuery()
                .eq(CompensationRecordEntity::getTenantId, tenantId)
                .eq(CompensationRecordEntity::getTaskId, taskId)
                .orderByDesc(CompensationRecordEntity::getCreatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    private CompensationRecord toDomain(CompensationRecordEntity entity) {
        return new CompensationRecord(
                entity.getId(),
                entity.getTenantId(),
                entity.getTaskId(),
                entity.getDiffRecordId(),
                entity.getActionType(),
                CompensationStatus.valueOf(entity.getStatus()),
                entity.getRequestPayload(),
                entity.getResultPayload(),
                entity.getOperatorId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private CompensationRecordEntity toEntity(CompensationRecord compensationRecord) {
        CompensationRecordEntity entity = new CompensationRecordEntity();
        entity.setId(compensationRecord.id());
        entity.setTenantId(compensationRecord.tenantId());
        entity.setTaskId(compensationRecord.taskId());
        entity.setDiffRecordId(compensationRecord.diffRecordId());
        entity.setActionType(compensationRecord.actionType());
        entity.setStatus(compensationRecord.status().name());
        entity.setRequestPayload(compensationRecord.requestPayload());
        entity.setResultPayload(compensationRecord.resultPayload());
        entity.setOperatorId(compensationRecord.operatorId());
        entity.setCreatedAt(compensationRecord.createdAt());
        entity.setUpdatedAt(compensationRecord.updatedAt());
        return entity;
    }
}
