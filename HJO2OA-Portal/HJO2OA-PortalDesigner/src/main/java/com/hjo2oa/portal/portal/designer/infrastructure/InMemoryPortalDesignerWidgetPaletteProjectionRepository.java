package com.hjo2oa.portal.portal.designer.infrastructure;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerWidgetPaletteProjection;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerWidgetPaletteProjectionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPortalDesignerWidgetPaletteProjectionRepository
        implements PortalDesignerWidgetPaletteProjectionRepository {

    private final Map<String, PortalDesignerWidgetPaletteProjection> projections = new ConcurrentHashMap<>();

    @Override
    public Optional<PortalDesignerWidgetPaletteProjection> findByWidgetId(String widgetId) {
        return Optional.ofNullable(projections.get(widgetId));
    }

    @Override
    public List<PortalDesignerWidgetPaletteProjection> findAll() {
        return List.copyOf(projections.values());
    }

    @Override
    public PortalDesignerWidgetPaletteProjection save(PortalDesignerWidgetPaletteProjection projection) {
        projections.put(projection.widgetId(), projection);
        return projection;
    }
}
