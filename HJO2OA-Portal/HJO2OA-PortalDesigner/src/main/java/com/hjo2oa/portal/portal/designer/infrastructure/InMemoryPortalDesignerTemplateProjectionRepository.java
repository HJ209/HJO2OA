package com.hjo2oa.portal.portal.designer.infrastructure;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjectionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPortalDesignerTemplateProjectionRepository implements PortalDesignerTemplateProjectionRepository {

    private final Map<String, PortalDesignerTemplateProjection> projections = new ConcurrentHashMap<>();

    @Override
    public Optional<PortalDesignerTemplateProjection> findByTemplateId(String templateId) {
        return Optional.ofNullable(projections.get(templateId));
    }

    @Override
    public List<PortalDesignerTemplateProjection> findAllByTenant(String tenantId) {
        return projections.values().stream()
                .filter(projection -> projection.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public PortalDesignerTemplateProjection save(PortalDesignerTemplateProjection projection) {
        projections.put(projection.templateId(), projection);
        return projection;
    }
}
