package com.hjo2oa.portal.portal.designer.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerWidgetPaletteEntryState;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerWidgetPaletteProjection;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerWidgetPaletteProjectionRepository;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisPortalDesignerWidgetPaletteProjectionRepository
        implements PortalDesignerWidgetPaletteProjectionRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final PortalDesignerWidgetPaletteProjectionMapper mapper;
    private final ObjectMapper objectMapper;

    public MybatisPortalDesignerWidgetPaletteProjectionRepository(
            PortalDesignerWidgetPaletteProjectionMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<PortalDesignerWidgetPaletteProjection> findByWidgetId(String widgetId) {
        return Optional.ofNullable(mapper.selectById(widgetId)).map(this::toDomain);
    }

    @Override
    public List<PortalDesignerWidgetPaletteProjection> findAll() {
        return mapper.selectList(new QueryWrapper<PortalDesignerWidgetPaletteProjectionEntity>()
                        .orderByAsc("widget_code"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PortalDesignerWidgetPaletteProjection save(PortalDesignerWidgetPaletteProjection projection) {
        PortalDesignerWidgetPaletteProjectionEntity existing = mapper.selectById(projection.widgetId());
        PortalDesignerWidgetPaletteProjectionEntity entity = toEntity(projection, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findByWidgetId(projection.widgetId()).orElseThrow();
    }

    private PortalDesignerWidgetPaletteProjection toDomain(PortalDesignerWidgetPaletteProjectionEntity entity) {
        return new PortalDesignerWidgetPaletteProjection(
                entity.getWidgetId(),
                entity.getWidgetCode(),
                WidgetCardType.valueOf(entity.getCardType()),
                entity.getSceneType() == null ? null : WidgetSceneType.valueOf(entity.getSceneType()),
                PortalDesignerWidgetPaletteEntryState.valueOf(entity.getState()),
                read(entity.getChangedFieldsJson()),
                entity.getTriggerEventType(),
                entity.getUpdatedAt()
        );
    }

    private PortalDesignerWidgetPaletteProjectionEntity toEntity(
            PortalDesignerWidgetPaletteProjection projection,
            PortalDesignerWidgetPaletteProjectionEntity existing
    ) {
        PortalDesignerWidgetPaletteProjectionEntity entity =
                existing == null ? new PortalDesignerWidgetPaletteProjectionEntity() : existing;
        entity.setWidgetId(projection.widgetId());
        entity.setWidgetCode(projection.widgetCode());
        entity.setCardType(projection.cardType().name());
        entity.setSceneType(projection.sceneType() == null ? null : projection.sceneType().name());
        entity.setState(projection.state().name());
        entity.setChangedFieldsJson(write(projection.changedFields()));
        entity.setTriggerEventType(projection.triggerEventType());
        entity.setUpdatedAt(projection.updatedAt());
        return entity;
    }

    private List<String> read(String json) {
        try {
            return json == null || json.isBlank() ? List.of() : objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid designer widget palette JSON", ex);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to write designer widget palette JSON", ex);
        }
    }
}
