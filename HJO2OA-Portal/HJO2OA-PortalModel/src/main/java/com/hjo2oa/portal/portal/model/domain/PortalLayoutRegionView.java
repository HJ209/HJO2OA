package com.hjo2oa.portal.portal.model.domain;

import java.util.List;

public record PortalLayoutRegionView(
        String regionId,
        String regionCode,
        String title,
        boolean required,
        List<PortalWidgetPlacementView> placements
) {
}
