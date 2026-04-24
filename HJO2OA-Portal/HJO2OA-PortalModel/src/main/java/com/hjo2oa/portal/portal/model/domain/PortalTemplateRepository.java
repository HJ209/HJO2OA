package com.hjo2oa.portal.portal.model.domain;

import java.util.List;
import java.util.Optional;

public interface PortalTemplateRepository {

    Optional<PortalTemplate> findByTemplateId(String templateId);

    Optional<PortalTemplate> findByTemplateCode(String tenantId, String templateCode);

    List<PortalTemplate> findAllByTenant(String tenantId);

    PortalTemplate save(PortalTemplate template);
}
