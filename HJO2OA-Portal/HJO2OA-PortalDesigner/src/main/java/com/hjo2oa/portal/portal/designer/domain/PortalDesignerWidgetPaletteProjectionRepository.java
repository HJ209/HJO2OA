package com.hjo2oa.portal.portal.designer.domain;

import java.util.List;
import java.util.Optional;

public interface PortalDesignerWidgetPaletteProjectionRepository {

    Optional<PortalDesignerWidgetPaletteProjection> findByWidgetId(String widgetId);

    List<PortalDesignerWidgetPaletteProjection> findAll();

    PortalDesignerWidgetPaletteProjection save(PortalDesignerWidgetPaletteProjection projection);
}
