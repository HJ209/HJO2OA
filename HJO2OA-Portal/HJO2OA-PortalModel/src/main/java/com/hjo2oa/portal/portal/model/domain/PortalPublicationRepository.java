package com.hjo2oa.portal.portal.model.domain;

import java.util.List;
import java.util.Optional;

public interface PortalPublicationRepository {

    Optional<PortalPublication> findByPublicationId(String publicationId);

    List<PortalPublication> findAllActiveByTenantAndSceneAndClient(
            String tenantId,
            PortalPublicationSceneType sceneType,
            PortalPublicationClientType clientType
    );

    List<PortalPublication> findAllByTenant(String tenantId);

    PortalPublication save(PortalPublication publication);
}
