package com.hjo2oa.portal.portal.model.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetReferenceState;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetReferenceStatus;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetReferenceStatusRepository;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisPortalWidgetReferenceStatusRepository implements PortalWidgetReferenceStatusRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final PortalWidgetReferenceStatusMapper mapper;
    private final ObjectMapper objectMapper;

    public MybatisPortalWidgetReferenceStatusRepository(
            PortalWidgetReferenceStatusMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<PortalWidgetReferenceStatus> findByWidgetId(String widgetId) {
        return Optional.ofNullable(mapper.selectById(widgetId)).map(this::toDomain);
    }

    @Override
    public Optional<PortalWidgetReferenceStatus> findByWidgetCode(String tenantId, String widgetCode) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<PortalWidgetReferenceStatusEntity>()
                .eq("tenant_id", tenantId)
                .eq("widget_code", widgetCode))).map(this::toDomain);
    }

    @Override
    public PortalWidgetReferenceStatus save(PortalWidgetReferenceStatus status) {
        PortalWidgetReferenceStatusEntity existing = mapper.selectById(status.widgetId());
        PortalWidgetReferenceStatusEntity entity = toEntity(status, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findByWidgetId(status.widgetId()).orElseThrow();
    }

    private PortalWidgetReferenceStatus toDomain(PortalWidgetReferenceStatusEntity entity) {
        return new PortalWidgetReferenceStatus(
                entity.getWidgetId(),
                entity.getTenantId(),
                entity.getWidgetCode(),
                WidgetCardType.valueOf(entity.getCardType()),
                entity.getSceneType() == null ? null : WidgetSceneType.valueOf(entity.getSceneType()),
                PortalWidgetReferenceState.valueOf(entity.getState()),
                read(entity.getChangedFieldsJson()),
                entity.getTriggerEventType(),
                entity.getUpdatedAt()
        );
    }

    private PortalWidgetReferenceStatusEntity toEntity(
            PortalWidgetReferenceStatus status,
            PortalWidgetReferenceStatusEntity existing
    ) {
        PortalWidgetReferenceStatusEntity entity = existing == null ? new PortalWidgetReferenceStatusEntity() : existing;
        entity.setWidgetId(status.widgetId());
        entity.setTenantId(status.tenantId());
        entity.setWidgetCode(status.widgetCode());
        entity.setCardType(status.cardType().name());
        entity.setSceneType(status.sceneType() == null ? null : status.sceneType().name());
        entity.setState(status.state().name());
        entity.setChangedFieldsJson(write(status.changedFields()));
        entity.setTriggerEventType(status.triggerEventType());
        entity.setUpdatedAt(status.updatedAt());
        return entity;
    }

    private List<String> read(String json) {
        try {
            return json == null || json.isBlank() ? List.of() : objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid widget reference JSON", ex);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to write widget reference JSON", ex);
        }
    }
}
