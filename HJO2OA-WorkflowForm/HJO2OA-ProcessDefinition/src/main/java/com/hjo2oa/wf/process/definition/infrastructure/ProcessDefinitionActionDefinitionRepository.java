package com.hjo2oa.wf.process.definition.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.wf.process.definition.domain.ActionCategory;
import com.hjo2oa.wf.process.definition.domain.ActionDefinition;
import com.hjo2oa.wf.process.definition.domain.ActionDefinitionRepository;
import com.hjo2oa.wf.process.definition.domain.RouteTarget;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class ProcessDefinitionActionDefinitionRepository implements ActionDefinitionRepository {

    private final ActionDefinitionMapper mapper;

    public ProcessDefinitionActionDefinitionRepository(ActionDefinitionMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Optional<ActionDefinition> findById(UUID actionId) {
        return Optional.ofNullable(mapper.selectById(actionId)).map(this::toDomain);
    }

    @Override
    public Optional<ActionDefinition> findByTenantAndCode(UUID tenantId, String code) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<ActionDefinitionEntity>lambdaQuery()
                        .eq(ActionDefinitionEntity::getTenantId, tenantId)
                        .eq(ActionDefinitionEntity::getCode, code)))
                .map(this::toDomain);
    }

    @Override
    public List<ActionDefinition> findByTenant(UUID tenantId) {
        return mapper.selectList(Wrappers.<ActionDefinitionEntity>lambdaQuery()
                        .eq(ActionDefinitionEntity::getTenantId, tenantId)
                        .orderByAsc(ActionDefinitionEntity::getCategory)
                        .orderByAsc(ActionDefinitionEntity::getCode))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ActionDefinition> findByTenantAndCategory(UUID tenantId, ActionCategory category) {
        return mapper.selectList(Wrappers.<ActionDefinitionEntity>lambdaQuery()
                        .eq(ActionDefinitionEntity::getTenantId, tenantId)
                        .eq(ActionDefinitionEntity::getCategory, category.name())
                        .orderByAsc(ActionDefinitionEntity::getCode))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public ActionDefinition save(ActionDefinition actionDefinition) {
        ActionDefinitionEntity existing = mapper.selectById(actionDefinition.id());
        ActionDefinitionEntity entity = toEntity(actionDefinition, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findById(actionDefinition.id()).orElseThrow();
    }

    @Override
    public void delete(UUID actionId) {
        mapper.deleteById(actionId);
    }

    private ActionDefinition toDomain(ActionDefinitionEntity entity) {
        return new ActionDefinition(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                ActionCategory.valueOf(entity.getCategory()),
                RouteTarget.valueOf(entity.getRouteTarget()),
                Boolean.TRUE.equals(entity.getRequireOpinion()),
                Boolean.TRUE.equals(entity.getRequireTarget()),
                entity.getUiConfig(),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ActionDefinitionEntity toEntity(ActionDefinition actionDefinition, ActionDefinitionEntity existing) {
        ActionDefinitionEntity entity = existing == null ? new ActionDefinitionEntity() : existing;
        entity.setId(actionDefinition.id());
        entity.setCode(actionDefinition.code());
        entity.setName(actionDefinition.name());
        entity.setCategory(actionDefinition.category().name());
        entity.setRouteTarget(actionDefinition.routeTarget().name());
        entity.setRequireOpinion(actionDefinition.requireOpinion());
        entity.setRequireTarget(actionDefinition.requireTarget());
        entity.setUiConfig(actionDefinition.uiConfig());
        entity.setTenantId(actionDefinition.tenantId());
        entity.setCreatedAt(actionDefinition.createdAt());
        entity.setUpdatedAt(actionDefinition.updatedAt());
        return entity;
    }
}
