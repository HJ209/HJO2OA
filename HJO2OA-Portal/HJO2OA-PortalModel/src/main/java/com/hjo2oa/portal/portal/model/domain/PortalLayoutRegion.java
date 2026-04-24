package com.hjo2oa.portal.portal.model.domain;

import java.util.List;
import java.util.Objects;

public record PortalLayoutRegion(
        String regionId,
        String regionCode,
        String title,
        boolean required,
        List<PortalWidgetPlacement> placements
) {

    public PortalLayoutRegion {
        regionId = requireText(regionId, "regionId");
        regionCode = requireText(regionCode, "regionCode");
        title = requireText(title, "title");
        placements = List.copyOf(Objects.requireNonNullElse(placements, List.of()));
    }

    public PortalLayoutRegionView toView() {
        return new PortalLayoutRegionView(
                regionId,
                regionCode,
                title,
                required,
                placements.stream().map(PortalWidgetPlacement::toView).toList()
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
