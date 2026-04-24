package com.hjo2oa.portal.portal.model.infrastructure;

import com.hjo2oa.portal.portal.model.domain.PortalPublication;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationRepository;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPortalPublicationRepository implements PortalPublicationRepository {

    private final Map<String, PortalPublication> publicationsById = new ConcurrentHashMap<>();

    @Override
    public Optional<PortalPublication> findByPublicationId(String publicationId) {
        return Optional.ofNullable(publicationsById.get(publicationId));
    }

    @Override
    public List<PortalPublication> findAllActiveByTenantAndSceneAndClient(
            String tenantId,
            PortalPublicationSceneType sceneType,
            PortalPublicationClientType clientType
    ) {
        return publicationsById.values().stream()
                .filter(publication -> publication.tenantId().equals(tenantId))
                .filter(publication -> publication.sceneType() == sceneType)
                .filter(publication -> publication.clientType() == clientType)
                .filter(publication -> publication.status() == PortalPublicationStatus.ACTIVE)
                .sorted(Comparator.comparing(PortalPublication::publicationId))
                .toList();
    }

    @Override
    public List<PortalPublication> findAllByTenant(String tenantId) {
        return publicationsById.values().stream()
                .filter(publication -> publication.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public PortalPublication save(PortalPublication publication) {
        publicationsById.put(publication.publicationId(), publication);
        return publication;
    }
}
