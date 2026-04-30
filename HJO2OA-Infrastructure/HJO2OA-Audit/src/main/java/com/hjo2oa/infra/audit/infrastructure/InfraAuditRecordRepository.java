package com.hjo2oa.infra.audit.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hjo2oa.infra.audit.domain.ArchiveStatus;
import com.hjo2oa.infra.audit.domain.AuditFieldChange;
import com.hjo2oa.infra.audit.domain.AuditQuery;
import com.hjo2oa.infra.audit.domain.AuditRecord;
import com.hjo2oa.infra.audit.domain.AuditRecordRepository;
import com.hjo2oa.infra.audit.domain.SensitivityLevel;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Primary
@Repository
public class InfraAuditRecordRepository implements AuditRecordRepository {

    private final AuditRecordMapper auditRecordMapper;
    private final AuditFieldChangeMapper auditFieldChangeMapper;

    public InfraAuditRecordRepository(
            AuditRecordMapper auditRecordMapper,
            AuditFieldChangeMapper auditFieldChangeMapper
    ) {
        this.auditRecordMapper = auditRecordMapper;
        this.auditFieldChangeMapper = auditFieldChangeMapper;
    }

    @Override
    @Transactional
    public AuditRecord save(AuditRecord record) {
        AuditRecordEntity entity = toEntity(record);
        if (auditRecordMapper.selectById(record.id()) == null) {
            auditRecordMapper.insert(entity);
        } else {
            auditRecordMapper.updateById(entity);
        }
        auditFieldChangeMapper.delete(new LambdaQueryWrapper<AuditFieldChangeEntity>()
                .eq(AuditFieldChangeEntity::getAuditRecordId, record.id()));
        for (AuditFieldChange fieldChange : record.fieldChanges()) {
            auditFieldChangeMapper.insert(toEntity(fieldChange));
        }
        return record;
    }

    @Override
    public Optional<AuditRecord> findById(UUID id) {
        AuditRecordEntity entity = auditRecordMapper.selectById(id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    public List<AuditRecord> findByQuery(AuditQuery query) {
        LambdaQueryWrapper<AuditRecordEntity> wrapper = new LambdaQueryWrapper<AuditRecordEntity>()
                .orderByDesc(AuditRecordEntity::getOccurredAt)
                .orderByDesc(AuditRecordEntity::getCreatedAt);
        if (query.tenantId() != null) {
            wrapper.eq(AuditRecordEntity::getTenantId, query.tenantId());
        }
        if (query.moduleCode() != null) {
            wrapper.eq(AuditRecordEntity::getModuleCode, query.moduleCode());
        }
        if (query.objectType() != null) {
            wrapper.eq(AuditRecordEntity::getObjectType, query.objectType());
        }
        if (query.objectId() != null) {
            wrapper.eq(AuditRecordEntity::getObjectId, query.objectId());
        }
        if (query.actionType() != null) {
            wrapper.eq(AuditRecordEntity::getActionType, query.actionType());
        }
        if (query.operatorAccountId() != null) {
            wrapper.eq(AuditRecordEntity::getOperatorAccountId, query.operatorAccountId());
        }
        if (query.operatorPersonId() != null) {
            wrapper.eq(AuditRecordEntity::getOperatorPersonId, query.operatorPersonId());
        }
        if (query.traceId() != null) {
            wrapper.eq(AuditRecordEntity::getTraceId, query.traceId());
        }
        if (query.from() != null) {
            wrapper.ge(AuditRecordEntity::getOccurredAt, toLocalDateTime(query.from()));
        }
        if (query.to() != null) {
            wrapper.le(AuditRecordEntity::getOccurredAt, toLocalDateTime(query.to()));
        }
        return auditRecordMapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public List<AuditRecord> findByTenantAndTimeRange(UUID tenantId, Instant from, Instant to) {
        LambdaQueryWrapper<AuditRecordEntity> wrapper = new LambdaQueryWrapper<AuditRecordEntity>()
                .eq(AuditRecordEntity::getTenantId, tenantId)
                .orderByAsc(AuditRecordEntity::getOccurredAt);
        if (from != null) {
            wrapper.ge(AuditRecordEntity::getOccurredAt, toLocalDateTime(from));
        }
        if (to != null) {
            wrapper.le(AuditRecordEntity::getOccurredAt, toLocalDateTime(to));
        }
        return auditRecordMapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    private AuditRecordEntity toEntity(AuditRecord record) {
        AuditRecordEntity entity = new AuditRecordEntity();
        entity.setId(record.id());
        entity.setModuleCode(record.moduleCode());
        entity.setObjectType(record.objectType());
        entity.setObjectId(record.objectId());
        entity.setActionType(record.actionType());
        entity.setOperatorAccountId(record.operatorAccountId());
        entity.setOperatorPersonId(record.operatorPersonId());
        entity.setTenantId(record.tenantId());
        entity.setTraceId(record.traceId());
        entity.setSummary(record.summary());
        entity.setOccurredAt(toLocalDateTime(record.occurredAt()));
        entity.setArchiveStatus(record.archiveStatus().name());
        entity.setCreatedAt(toLocalDateTime(record.createdAt()));
        return entity;
    }

    private AuditFieldChangeEntity toEntity(AuditFieldChange fieldChange) {
        AuditFieldChangeEntity entity = new AuditFieldChangeEntity();
        entity.setId(fieldChange.id());
        entity.setAuditRecordId(fieldChange.auditRecordId());
        entity.setFieldName(fieldChange.fieldName());
        entity.setOldValue(fieldChange.oldValue());
        entity.setNewValue(fieldChange.newValue());
        entity.setSensitivityLevel(fieldChange.sensitivityLevel() == null ? null : fieldChange.sensitivityLevel().name());
        return entity;
    }

    private AuditRecord toDomain(AuditRecordEntity entity) {
        List<AuditFieldChange> fieldChanges = auditFieldChangeMapper.selectList(
                new LambdaQueryWrapper<AuditFieldChangeEntity>()
                        .eq(AuditFieldChangeEntity::getAuditRecordId, entity.getId())
                        .orderByAsc(AuditFieldChangeEntity::getFieldName)
        ).stream().map(this::toDomain).toList();
        return new AuditRecord(
                entity.getId(),
                entity.getModuleCode(),
                entity.getObjectType(),
                entity.getObjectId(),
                entity.getActionType(),
                entity.getOperatorAccountId(),
                entity.getOperatorPersonId(),
                entity.getTenantId(),
                entity.getTraceId(),
                entity.getSummary(),
                toInstant(entity.getOccurredAt()),
                ArchiveStatus.valueOf(entity.getArchiveStatus()),
                toInstant(entity.getCreatedAt()),
                fieldChanges
        );
    }

    private AuditFieldChange toDomain(AuditFieldChangeEntity entity) {
        return new AuditFieldChange(
                entity.getId(),
                entity.getAuditRecordId(),
                entity.getFieldName(),
                entity.getOldValue(),
                entity.getNewValue(),
                entity.getSensitivityLevel() == null ? null : SensitivityLevel.valueOf(entity.getSensitivityLevel())
        );
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.UTC);
    }
}
