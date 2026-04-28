package com.hjo2oa.portal.portal.model.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.portal.portal.model.domain.PortalPage;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplate;
import com.hjo2oa.portal.portal.model.domain.PortalTemplatePublishedCanvasSnapshot;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateRepository;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisPortalTemplateRepository implements PortalTemplateRepository {

    private static final TypeReference<List<PortalPage>> PAGE_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<PortalTemplateVersion>> VERSION_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<PortalTemplatePublishedCanvasSnapshot>> SNAPSHOT_LIST = new TypeReference<>() {
    };

    private final PortalTemplateMapper mapper;
    private final ObjectMapper objectMapper;

    public MybatisPortalTemplateRepository(PortalTemplateMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<PortalTemplate> findByTemplateId(String templateId) {
        return Optional.ofNullable(mapper.selectById(templateId)).map(this::toDomain);
    }

    @Override
    public Optional<PortalTemplate> findByTemplateCode(String tenantId, String templateCode) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<PortalTemplateEntity>()
                .eq("tenant_id", tenantId)
                .eq("template_code", templateCode))).map(this::toDomain);
    }

    @Override
    public List<PortalTemplate> findAllByTenant(String tenantId) {
        return mapper.selectList(new QueryWrapper<PortalTemplateEntity>()
                        .eq("tenant_id", tenantId)
                        .orderByAsc("template_code"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PortalTemplate save(PortalTemplate template) {
        PortalTemplateEntity existing = mapper.selectById(template.templateId());
        PortalTemplateEntity entity = toEntity(template, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findByTemplateId(template.templateId()).orElseThrow();
    }

    private PortalTemplate toDomain(PortalTemplateEntity entity) {
        return new PortalTemplate(
                entity.getTemplateId(),
                entity.getTenantId(),
                entity.getTemplateCode(),
                entity.getDisplayName(),
                PortalPublicationSceneType.valueOf(entity.getSceneType()),
                read(entity.getPagesJson(), PAGE_LIST),
                read(entity.getVersionsJson(), VERSION_LIST),
                read(entity.getPublishedSnapshotsJson(), SNAPSHOT_LIST),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PortalTemplateEntity toEntity(PortalTemplate template, PortalTemplateEntity existing) {
        PortalTemplateEntity entity = existing == null ? new PortalTemplateEntity() : existing;
        entity.setTemplateId(template.templateId());
        entity.setTenantId(template.tenantId());
        entity.setTemplateCode(template.templateCode());
        entity.setDisplayName(template.displayName());
        entity.setSceneType(template.sceneType().name());
        entity.setPagesJson(write(template.pages()));
        entity.setVersionsJson(write(template.versions()));
        entity.setPublishedSnapshotsJson(write(template.publishedSnapshots()));
        entity.setCreatedAt(template.createdAt());
        entity.setUpdatedAt(template.updatedAt());
        return entity;
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid portal template JSON", ex);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to write portal template JSON", ex);
        }
    }
}
