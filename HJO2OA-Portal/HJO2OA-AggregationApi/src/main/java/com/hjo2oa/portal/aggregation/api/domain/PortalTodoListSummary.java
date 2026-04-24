package com.hjo2oa.portal.aggregation.api.domain;

public record PortalTodoListSummary(
        long pendingCount,
        long completedCount,
        long overdueCount,
        long copiedUnreadCount
) {
}
