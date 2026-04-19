package com.hjo2oa.portal.aggregation.api.domain;

import java.util.List;
import java.util.Map;

public record PortalTodoCard(
        long totalCount,
        long urgentCount,
        Map<String, Long> categoryStats,
        List<PortalTodoItem> topItems
) {

    public PortalTodoCard {
        categoryStats = categoryStats == null ? Map.of() : Map.copyOf(categoryStats);
        topItems = topItems == null ? List.of() : List.copyOf(topItems);
    }

    public static PortalTodoCard empty() {
        return new PortalTodoCard(0, 0, Map.of(), List.of());
    }

    public boolean isEmpty() {
        return totalCount == 0 && topItems.isEmpty();
    }
}
