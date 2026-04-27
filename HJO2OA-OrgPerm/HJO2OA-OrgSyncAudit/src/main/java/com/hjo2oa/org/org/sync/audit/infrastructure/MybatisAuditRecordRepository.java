package com.hjo2oa.org.org.sync.audit.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.org.org.sync.audit.domain.AuditCategory;
import com.hjo2oa.org.org.sync.audit.domain.AuditRecord;
import com.hjo2oa.org.org.sync.audit.domain.AuditRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisAuditRecordRepository implements AuditRecordRepository {

    private final AuditRecordMapper mapper;

    public MybatisAuditRecordRepository(AuditRecordMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public AuditRecord save(AuditRecord auditRecord) {
        AuditRecordEntity entity = toEntity(auditRecord);
        if (mapper.selectById(auditRecord.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return auditRecord;
    }

    @Override
    public List<AuditRecord> findByQuery(
            UUID tenantId,
            AuditCategory category,
            String entityType,
            String entityId,
            UUID taskId,
            Instant from,
            Instant to
    ) {
        LambdaQueryWrapper<AuditRecordEntity> wrapper = Wrappers.<AuditRecordEntity>lambdaQuery()
                .eq(AuditRecordEntity::getTenantId, tenantId)
                .orderByDesc(AuditRecordEntity::getOccurredAt)
                .orderByDesc(AuditRecordEntity::getCreatedAt);
        if (category != null) {
            wrapper.eq(AuditRecordEntity::getCategory, category.name());
        }
        if (entityType != null) {
            wrapper.eq(AuditRecordEntity::getEntityType, entityType);
        }
        if (entityId != null) {
            wrapper.eq(AuditRecordEntity::getEntityId, entityId);
        }
        if (taskId != null) {
            wrapper.eq(AuditRecordEntity::getTaskId, taskId);
        }
        if (from != null) {
            wrapper.ge(AuditRecordEntity::getOccurredAt, from);
        }
        if (to != null) {
            wrapper.le(AuditRecordEntity::getOccurredAt, to);
        }
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    private AuditRecord toDomain(AuditRecordEntity entity) {
        return new AuditRecord(
                entity.getId(),
                entity.getTenantId(),
                AuditCategory.valueOf(entity.getCategory()),
                entity.getActionType(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getTaskId(),
                entity.getTriggerSource(),
                entity.getOperatorId(),
                entity.getBeforeSnapshot(),
                entity.getAfterSnapshot(),
                entity.getSummary(),
                entity.getOccurredAt(),
                entity.getCreatedAt()
        );
    }

    private AuditRecordEntity toEntity(AuditRecord auditRecord) {
        AuditRecordEntity entity = new AuditRecordEntity();
        entity.setId(auditRecord.id());
        entity.setTenantId(auditRecord.tenantId());
        entity.setCategory(auditRecord.category().name());
        entity.setActionType(auditRecord.actionType());
        entity.setEntityType(auditRecord.entityType());
        entity.setEntityId(auditRecord.entityId());
        entity.setTaskId(auditRecord.taskId());
        entity.setTriggerSource(auditRecord.triggerSource());
        entity.setOperatorId(auditRecord.operatorId());
        entity.setBeforeSnapshot(auditRecord.beforeSnapshot());
        entity.setAfterSnapshot(auditRecord.afterSnapshot());
        entity.setSummary(auditRecord.summary());
        entity.setOccurredAt(auditRecord.occurredAt());
        entity.setCreatedAt(auditRecord.createdAt());
        return entity;
    }
}
