package com.hjo2oa.portal.portal.model.domain;

import java.util.List;

public record PortalPageView(
        String pageId,
        String pageCode,
        String title,
        boolean defaultPage,
        PortalTemplateLayoutMode layoutMode,
        List<PortalLayoutRegionView> regions
) {
}
