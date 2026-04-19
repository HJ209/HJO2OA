package com.hjo2oa.todo.center.domain;

public record TodoCounts(
        long pendingCount,
        long completedCount,
        long overdueCount,
        long initiatedCount,
        long copiedUnreadCount,
        long draftCount,
        long archivedCount
) {
}
