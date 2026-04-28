package com.hjo2oa.portal.portal.model.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjo2oa.portal.portal.model.domain.PortalPublication;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudienceType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationRepository;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisPortalPublicationRepository implements PortalPublicationRepository {

    private final PortalPublicationMapper mapper;

    public MybatisPortalPublicationRepository(PortalPublicationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<PortalPublication> findByPublicationId(String publicationId) {
        return Optional.ofNullable(mapper.selectById(publicationId)).map(this::toDomain);
    }

    @Override
    public List<PortalPublication> findAllActiveByTenantAndSceneAndClient(
            String tenantId,
            PortalPublicationSceneType sceneType,
            PortalPublicationClientType clientType
    ) {
        return mapper.selectList(new QueryWrapper<PortalPublicationEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("scene_type", sceneType.name())
                        .eq("client_type", clientType.name())
                        .eq("status", PortalPublicationStatus.ACTIVE.name())
                        .orderByAsc("publication_id"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PortalPublication> findAllByTenant(String tenantId) {
        return mapper.selectList(new QueryWrapper<PortalPublicationEntity>()
                        .eq("tenant_id", tenantId)
                        .orderByAsc("publication_id"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PortalPublication save(PortalPublication publication) {
        PortalPublicationEntity existing = mapper.selectById(publication.publicationId());
        PortalPublicationEntity entity = toEntity(publication, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findByPublicationId(publication.publicationId()).orElseThrow();
    }

    private PortalPublication toDomain(PortalPublicationEntity entity) {
        return new PortalPublication(
                entity.getPublicationId(),
                entity.getTenantId(),
                entity.getTemplateId(),
                PortalPublicationSceneType.valueOf(entity.getSceneType()),
                PortalPublicationClientType.valueOf(entity.getClientType()),
                new PortalPublicationAudience(
                        PortalPublicationAudienceType.valueOf(entity.getAudienceType()),
                        entity.getAudienceSubjectId()
                ),
                PortalPublicationStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getActivatedAt(),
                entity.getOfflinedAt()
        );
    }

    private PortalPublicationEntity toEntity(PortalPublication publication, PortalPublicationEntity existing) {
        PortalPublicationEntity entity = existing == null ? new PortalPublicationEntity() : existing;
        entity.setPublicationId(publication.publicationId());
        entity.setTenantId(publication.tenantId());
        entity.setTemplateId(publication.templateId());
        entity.setSceneType(publication.sceneType().name());
        entity.setClientType(publication.clientType().name());
        entity.setAudienceType(publication.audience().type().name());
        entity.setAudienceSubjectId(publication.audience().subjectId());
        entity.setStatus(publication.status().name());
        entity.setCreatedAt(publication.createdAt());
        entity.setUpdatedAt(publication.updatedAt());
        entity.setActivatedAt(publication.activatedAt());
        entity.setOfflinedAt(publication.offlinedAt());
        return entity;
    }
}
