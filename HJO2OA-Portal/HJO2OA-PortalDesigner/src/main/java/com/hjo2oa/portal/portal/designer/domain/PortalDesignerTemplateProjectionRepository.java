package com.hjo2oa.portal.portal.designer.domain;

import java.util.List;
import java.util.Optional;

public interface PortalDesignerTemplateProjectionRepository {

    Optional<PortalDesignerTemplateProjection> findByTemplateId(String templateId);

    List<PortalDesignerTemplateProjection> findAllByTenant(String tenantId);

    PortalDesignerTemplateProjection save(PortalDesignerTemplateProjection projection);
}
