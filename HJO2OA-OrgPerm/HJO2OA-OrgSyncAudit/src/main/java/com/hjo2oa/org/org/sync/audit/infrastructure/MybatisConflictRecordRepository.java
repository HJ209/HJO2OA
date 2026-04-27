package com.hjo2oa.org.org.sync.audit.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.org.org.sync.audit.domain.ConflictRecord;
import com.hjo2oa.org.org.sync.audit.domain.ConflictRecordRepository;
import com.hjo2oa.org.org.sync.audit.domain.ConflictSeverity;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisConflictRecordRepository implements ConflictRecordRepository {

    private final ConflictRecordMapper mapper;

    public MybatisConflictRecordRepository(ConflictRecordMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public ConflictRecord save(ConflictRecord conflictRecord) {
        ConflictRecordEntity entity = toEntity(conflictRecord);
        if (mapper.selectById(conflictRecord.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return conflictRecord;
    }

    @Override
    public List<ConflictRecord> findByDiffRecordId(UUID tenantId, UUID diffRecordId) {
        LambdaQueryWrapper<ConflictRecordEntity> wrapper = Wrappers.<ConflictRecordEntity>lambdaQuery()
                .eq(ConflictRecordEntity::getTenantId, tenantId)
                .eq(ConflictRecordEntity::getDiffRecordId, diffRecordId)
                .orderByAsc(ConflictRecordEntity::getConflictField);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    private ConflictRecord toDomain(ConflictRecordEntity entity) {
        return new ConflictRecord(
                entity.getId(),
                entity.getTenantId(),
                entity.getDiffRecordId(),
                entity.getConflictField(),
                entity.getSourceValue(),
                entity.getLocalValue(),
                ConflictSeverity.valueOf(entity.getSeverity()),
                entity.getCreatedAt()
        );
    }

    private ConflictRecordEntity toEntity(ConflictRecord conflictRecord) {
        ConflictRecordEntity entity = new ConflictRecordEntity();
        entity.setId(conflictRecord.id());
        entity.setTenantId(conflictRecord.tenantId());
        entity.setDiffRecordId(conflictRecord.diffRecordId());
        entity.setConflictField(conflictRecord.conflictField());
        entity.setSourceValue(conflictRecord.sourceValue());
        entity.setLocalValue(conflictRecord.localValue());
        entity.setSeverity(conflictRecord.severity().name());
        entity.setCreatedAt(conflictRecord.createdAt());
        return entity;
    }
}
