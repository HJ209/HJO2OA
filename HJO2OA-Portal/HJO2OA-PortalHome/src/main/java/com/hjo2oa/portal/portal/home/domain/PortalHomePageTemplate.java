package com.hjo2oa.portal.portal.home.domain;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record PortalHomePageTemplate(
        PortalHomeSceneType sceneType,
        PortalHomeLayoutType layoutType,
        PortalHomeBranding branding,
        List<PortalHomeNavigationItem> navigation,
        List<PortalHomeRegionTemplate> regions,
        PortalHomeFooter footer
) {

    public PortalHomePageTemplate {
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Objects.requireNonNull(layoutType, "layoutType must not be null");
        Objects.requireNonNull(branding, "branding must not be null");
        navigation = List.copyOf(Objects.requireNonNullElse(navigation, List.of()));
        regions = List.copyOf(Objects.requireNonNullElse(regions, List.of()));
        Objects.requireNonNull(footer, "footer must not be null");
    }

    public Set<com.hjo2oa.portal.aggregation.api.domain.PortalCardType> requestedCardTypes() {
        return regions.stream()
                .flatMap(region -> region.cards().stream())
                .map(PortalHomeCardTemplate::cardType)
                .collect(Collectors.toUnmodifiableSet());
    }
}
