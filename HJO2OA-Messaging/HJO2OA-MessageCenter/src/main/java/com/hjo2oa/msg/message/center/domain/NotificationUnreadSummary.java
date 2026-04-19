package com.hjo2oa.msg.message.center.domain;

import java.util.List;
import java.util.Map;

public record NotificationUnreadSummary(
        long totalUnreadCount,
        Map<String, Long> categoryUnreadCounts,
        List<String> latestNotificationIds
) {

    public NotificationUnreadSummary {
        categoryUnreadCounts = Map.copyOf(categoryUnreadCounts);
        latestNotificationIds = List.copyOf(latestNotificationIds);
    }
}
