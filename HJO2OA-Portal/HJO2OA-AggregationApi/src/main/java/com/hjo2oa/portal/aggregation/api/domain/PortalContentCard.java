package com.hjo2oa.portal.aggregation.api.domain;

import java.util.List;

public record PortalContentCard(
        long totalCount,
        List<PortalContentItem> latestArticles
) {

    public PortalContentCard {
        latestArticles = latestArticles == null ? List.of() : List.copyOf(latestArticles);
    }

    public static PortalContentCard empty() {
        return new PortalContentCard(0, List.of());
    }

    public boolean isEmpty() {
        return totalCount == 0 && latestArticles.isEmpty();
    }
}
