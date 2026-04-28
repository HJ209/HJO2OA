package com.hjo2oa.portal.portal.designer.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateVersionView;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisPortalDesignerTemplateProjectionRepository
        implements PortalDesignerTemplateProjectionRepository {

    private static final TypeReference<List<PortalDesignerTemplateVersionView>> VERSION_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final PortalDesignerTemplateProjectionMapper mapper;
    private final ObjectMapper objectMapper;

    public MybatisPortalDesignerTemplateProjectionRepository(
            PortalDesignerTemplateProjectionMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<PortalDesignerTemplateProjection> findByTemplateId(String templateId) {
        return Optional.ofNullable(mapper.selectById(templateId)).map(this::toDomain);
    }

    @Override
    public List<PortalDesignerTemplateProjection> findAllByTenant(String tenantId) {
        return mapper.selectList(new QueryWrapper<PortalDesignerTemplateProjectionEntity>()
                        .eq("tenant_id", tenantId)
                        .orderByAsc("template_code"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PortalDesignerTemplateProjection save(PortalDesignerTemplateProjection projection) {
        PortalDesignerTemplateProjectionEntity existing = mapper.selectById(projection.templateId());
        PortalDesignerTemplateProjectionEntity entity = toEntity(projection, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findByTemplateId(projection.templateId()).orElseThrow();
    }

    private PortalDesignerTemplateProjection toDomain(PortalDesignerTemplateProjectionEntity entity) {
        return new PortalDesignerTemplateProjection(
                entity.getTenantId(),
                entity.getTemplateId(),
                entity.getTemplateCode(),
                PortalPublicationSceneType.valueOf(entity.getSceneType()),
                read(entity.getVersionsJson(), VERSION_LIST),
                read(entity.getActivePublicationIdsJson(), STRING_LIST),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PortalDesignerTemplateProjectionEntity toEntity(
            PortalDesignerTemplateProjection projection,
            PortalDesignerTemplateProjectionEntity existing
    ) {
        PortalDesignerTemplateProjectionEntity entity =
                existing == null ? new PortalDesignerTemplateProjectionEntity() : existing;
        entity.setTemplateId(projection.templateId());
        entity.setTenantId(projection.tenantId());
        entity.setTemplateCode(projection.templateCode());
        entity.setSceneType(projection.sceneType().name());
        entity.setVersionsJson(write(projection.versions()));
        entity.setActivePublicationIdsJson(write(projection.activePublicationIds()));
        entity.setCreatedAt(projection.createdAt());
        entity.setUpdatedAt(projection.updatedAt());
        return entity;
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return json == null || json.isBlank() ? objectMapper.readValue("[]", type) : objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid designer template projection JSON", ex);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to write designer template projection JSON", ex);
        }
    }
}
