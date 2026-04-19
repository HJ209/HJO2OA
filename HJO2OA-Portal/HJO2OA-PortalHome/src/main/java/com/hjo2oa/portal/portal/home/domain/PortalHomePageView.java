package com.hjo2oa.portal.portal.home.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PortalHomePageView(
        PortalHomeSceneType sceneType,
        PortalHomeLayoutType layoutType,
        PortalHomeBranding branding,
        List<PortalHomeNavigationItem> navigation,
        List<PortalHomeRegionView> regions,
        PortalHomeFooter footer,
        Instant assembledAt
) {

    public PortalHomePageView {
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Objects.requireNonNull(layoutType, "layoutType must not be null");
        Objects.requireNonNull(branding, "branding must not be null");
        navigation = List.copyOf(Objects.requireNonNullElse(navigation, List.of()));
        regions = List.copyOf(Objects.requireNonNullElse(regions, List.of()));
        Objects.requireNonNull(footer, "footer must not be null");
        Objects.requireNonNull(assembledAt, "assembledAt must not be null");
    }
}
