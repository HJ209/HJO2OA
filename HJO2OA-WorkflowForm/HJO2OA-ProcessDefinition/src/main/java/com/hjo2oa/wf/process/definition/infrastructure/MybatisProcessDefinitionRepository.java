package com.hjo2oa.wf.process.definition.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.wf.process.definition.domain.DefinitionStatus;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinition;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinitionRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisProcessDefinitionRepository implements ProcessDefinitionRepository {

    private final ProcessDefinitionMapper mapper;

    public MybatisProcessDefinitionRepository(ProcessDefinitionMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Optional<ProcessDefinition> findById(UUID definitionId) {
        return Optional.ofNullable(mapper.selectById(definitionId)).map(this::toDomain);
    }

    @Override
    public List<ProcessDefinition> findByCode(UUID tenantId, String code) {
        return mapper.selectList(Wrappers.<ProcessDefinitionEntity>lambdaQuery()
                        .eq(ProcessDefinitionEntity::getTenantId, tenantId)
                        .eq(ProcessDefinitionEntity::getCode, code)
                        .orderByDesc(ProcessDefinitionEntity::getVersion))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ProcessDefinition> findByCodeAndVersion(UUID tenantId, String code, int version) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<ProcessDefinitionEntity>lambdaQuery()
                        .eq(ProcessDefinitionEntity::getTenantId, tenantId)
                        .eq(ProcessDefinitionEntity::getCode, code)
                        .eq(ProcessDefinitionEntity::getVersion, version)))
                .map(this::toDomain);
    }

    @Override
    public List<ProcessDefinition> findByTenantCategoryAndStatus(
            UUID tenantId,
            String category,
            DefinitionStatus status
    ) {
        LambdaQueryWrapper<ProcessDefinitionEntity> wrapper = Wrappers.<ProcessDefinitionEntity>lambdaQuery()
                .eq(ProcessDefinitionEntity::getTenantId, tenantId);
        if (category != null && !category.isBlank()) {
            wrapper.eq(ProcessDefinitionEntity::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(ProcessDefinitionEntity::getStatus, status.name());
        }
        wrapper.orderByAsc(ProcessDefinitionEntity::getCode)
                .orderByDesc(ProcessDefinitionEntity::getVersion);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public ProcessDefinition save(ProcessDefinition definition) {
        ProcessDefinitionEntity existing = mapper.selectById(definition.id());
        ProcessDefinitionEntity entity = toEntity(definition, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findById(definition.id()).orElseThrow();
    }

    @Override
    public void delete(UUID definitionId) {
        mapper.deleteById(definitionId);
    }

    private ProcessDefinition toDomain(ProcessDefinitionEntity entity) {
        return new ProcessDefinition(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getCategory(),
                entity.getVersion() == null ? 1 : entity.getVersion(),
                DefinitionStatus.valueOf(entity.getStatus()),
                entity.getFormMetadataId(),
                entity.getStartNodeId(),
                entity.getEndNodeId(),
                entity.getNodes(),
                entity.getRoutes(),
                entity.getTenantId(),
                entity.getPublishedAt(),
                entity.getPublishedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ProcessDefinitionEntity toEntity(ProcessDefinition definition, ProcessDefinitionEntity existing) {
        ProcessDefinitionEntity entity = existing == null ? new ProcessDefinitionEntity() : existing;
        entity.setId(definition.id());
        entity.setCode(definition.code());
        entity.setName(definition.name());
        entity.setCategory(definition.category());
        entity.setVersion(definition.version());
        entity.setStatus(definition.status().name());
        entity.setFormMetadataId(definition.formMetadataId());
        entity.setStartNodeId(definition.startNodeId());
        entity.setEndNodeId(definition.endNodeId());
        entity.setNodes(definition.nodes());
        entity.setRoutes(definition.routes());
        entity.setTenantId(definition.tenantId());
        entity.setPublishedAt(definition.publishedAt());
        entity.setPublishedBy(definition.publishedBy());
        entity.setCreatedAt(definition.createdAt());
        entity.setUpdatedAt(definition.updatedAt());
        return entity;
    }
}
