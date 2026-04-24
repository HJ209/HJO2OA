package com.hjo2oa.portal.portal.model.domain;

import java.util.List;
import java.util.Objects;

public record PortalPage(
        String pageId,
        String pageCode,
        String title,
        boolean defaultPage,
        PortalTemplateLayoutMode layoutMode,
        List<PortalLayoutRegion> regions
) {

    public PortalPage {
        pageId = requireText(pageId, "pageId");
        pageCode = requireText(pageCode, "pageCode");
        title = requireText(title, "title");
        Objects.requireNonNull(layoutMode, "layoutMode must not be null");
        regions = List.copyOf(Objects.requireNonNullElse(regions, List.of()));
    }

    public PortalPageView toView() {
        return new PortalPageView(
                pageId,
                pageCode,
                title,
                defaultPage,
                layoutMode,
                regions.stream().map(PortalLayoutRegion::toView).toList()
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
