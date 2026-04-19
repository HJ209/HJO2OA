package com.hjo2oa.portal.aggregation.api.domain;

import java.util.List;
import java.util.Map;

public record PortalMessageCard(
        long unreadCount,
        Map<String, Long> categoryStats,
        List<PortalMessageItem> topItems
) {

    public PortalMessageCard {
        categoryStats = categoryStats == null ? Map.of() : Map.copyOf(categoryStats);
        topItems = topItems == null ? List.of() : List.copyOf(topItems);
    }

    public static PortalMessageCard empty() {
        return new PortalMessageCard(0, Map.of(), List.of());
    }

    public boolean isEmpty() {
        return unreadCount == 0 && topItems.isEmpty();
    }
}
