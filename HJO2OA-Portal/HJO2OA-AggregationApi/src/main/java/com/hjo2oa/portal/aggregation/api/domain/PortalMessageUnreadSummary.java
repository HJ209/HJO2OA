package com.hjo2oa.portal.aggregation.api.domain;

import java.util.List;
import java.util.Map;

public record PortalMessageUnreadSummary(
        long totalUnreadCount,
        Map<String, Long> categoryUnreadCounts,
        List<String> latestNotificationIds
) {

    public PortalMessageUnreadSummary {
        categoryUnreadCounts = categoryUnreadCounts == null ? Map.of() : Map.copyOf(categoryUnreadCounts);
        latestNotificationIds = latestNotificationIds == null ? List.of() : List.copyOf(latestNotificationIds);
    }

    public static PortalMessageUnreadSummary empty() {
        return new PortalMessageUnreadSummary(0, Map.of(), List.of());
    }
}
