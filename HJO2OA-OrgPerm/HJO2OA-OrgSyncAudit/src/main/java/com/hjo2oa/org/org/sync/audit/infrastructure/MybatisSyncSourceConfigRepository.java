package com.hjo2oa.org.org.sync.audit.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.org.org.sync.audit.domain.SourceStatus;
import com.hjo2oa.org.org.sync.audit.domain.SyncSourceConfig;
import com.hjo2oa.org.org.sync.audit.domain.SyncSourceConfigRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisSyncSourceConfigRepository implements SyncSourceConfigRepository {

    private final SyncSourceConfigMapper mapper;

    public MybatisSyncSourceConfigRepository(SyncSourceConfigMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public SyncSourceConfig save(SyncSourceConfig config) {
        SyncSourceConfigEntity entity = toEntity(config);
        if (mapper.selectById(config.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findById(config.id()).orElseThrow();
    }

    @Override
    public Optional<SyncSourceConfig> findById(UUID sourceId) {
        return Optional.ofNullable(mapper.selectById(sourceId)).map(this::toDomain);
    }

    @Override
    public Optional<SyncSourceConfig> findByTenantIdAndSourceCode(UUID tenantId, String sourceCode) {
        LambdaQueryWrapper<SyncSourceConfigEntity> wrapper = Wrappers.<SyncSourceConfigEntity>lambdaQuery()
                .eq(SyncSourceConfigEntity::getTenantId, tenantId)
                .eq(SyncSourceConfigEntity::getSourceCode, sourceCode);
        return Optional.ofNullable(mapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public List<SyncSourceConfig> findByTenantId(UUID tenantId) {
        LambdaQueryWrapper<SyncSourceConfigEntity> wrapper = Wrappers.<SyncSourceConfigEntity>lambdaQuery()
                .eq(SyncSourceConfigEntity::getTenantId, tenantId)
                .orderByAsc(SyncSourceConfigEntity::getSourceCode);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    private SyncSourceConfig toDomain(SyncSourceConfigEntity entity) {
        return new SyncSourceConfig(
                entity.getId(),
                entity.getTenantId(),
                entity.getSourceCode(),
                entity.getSourceName(),
                entity.getSourceType(),
                entity.getEndpoint(),
                entity.getConfigRef(),
                entity.getScopeExpression(),
                SourceStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private SyncSourceConfigEntity toEntity(SyncSourceConfig source) {
        SyncSourceConfigEntity entity = new SyncSourceConfigEntity();
        entity.setId(source.id());
        entity.setTenantId(source.tenantId());
        entity.setSourceCode(source.sourceCode());
        entity.setSourceName(source.sourceName());
        entity.setSourceType(source.sourceType());
        entity.setEndpoint(source.endpoint());
        entity.setConfigRef(source.configRef());
        entity.setScopeExpression(source.scopeExpression());
        entity.setStatus(source.status().name());
        entity.setCreatedAt(source.createdAt());
        entity.setUpdatedAt(source.updatedAt());
        return entity;
    }
}
