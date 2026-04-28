package com.hjo2oa.portal.widget.config.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetDataSourceType;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinition;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionRepository;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionStatus;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisWidgetDefinitionRepository implements WidgetDefinitionRepository {

    private final WidgetDefinitionMapper mapper;

    public MybatisWidgetDefinitionRepository(WidgetDefinitionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<WidgetDefinition> findByWidgetId(String widgetId) {
        return Optional.ofNullable(mapper.selectById(widgetId)).map(this::toDomain);
    }

    @Override
    public Optional<WidgetDefinition> findByWidgetCode(String tenantId, String widgetCode) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<WidgetDefinitionEntity>()
                .eq("tenant_id", tenantId)
                .eq("widget_code", widgetCode))).map(this::toDomain);
    }

    @Override
    public List<WidgetDefinition> findAllByTenant(String tenantId) {
        return mapper.selectList(new QueryWrapper<WidgetDefinitionEntity>()
                        .eq("tenant_id", tenantId)
                        .orderByAsc("widget_code"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public WidgetDefinition save(WidgetDefinition widgetDefinition) {
        WidgetDefinitionEntity existing = mapper.selectById(widgetDefinition.widgetId());
        WidgetDefinitionEntity entity = toEntity(widgetDefinition, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findByWidgetId(widgetDefinition.widgetId()).orElseThrow();
    }

    private WidgetDefinition toDomain(WidgetDefinitionEntity entity) {
        return new WidgetDefinition(
                entity.getWidgetId(),
                entity.getTenantId(),
                entity.getWidgetCode(),
                entity.getDisplayName(),
                WidgetCardType.valueOf(entity.getCardType()),
                entity.getSceneType() == null ? null : WidgetSceneType.valueOf(entity.getSceneType()),
                entity.getSourceModule(),
                WidgetDataSourceType.valueOf(entity.getDataSourceType()),
                Boolean.TRUE.equals(entity.getAllowHide()),
                Boolean.TRUE.equals(entity.getAllowCollapse()),
                entity.getMaxItems(),
                WidgetDefinitionStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private WidgetDefinitionEntity toEntity(WidgetDefinition definition, WidgetDefinitionEntity existing) {
        WidgetDefinitionEntity entity = existing == null ? new WidgetDefinitionEntity() : existing;
        entity.setWidgetId(definition.widgetId());
        entity.setTenantId(definition.tenantId());
        entity.setWidgetCode(definition.widgetCode());
        entity.setDisplayName(definition.displayName());
        entity.setCardType(definition.cardType().name());
        entity.setSceneType(definition.sceneType() == null ? null : definition.sceneType().name());
        entity.setSourceModule(definition.sourceModule());
        entity.setDataSourceType(definition.dataSourceType().name());
        entity.setAllowHide(definition.allowHide());
        entity.setAllowCollapse(definition.allowCollapse());
        entity.setMaxItems(definition.maxItems());
        entity.setStatus(definition.status().name());
        entity.setCreatedAt(definition.createdAt());
        entity.setUpdatedAt(definition.updatedAt());
        return entity;
    }
}
