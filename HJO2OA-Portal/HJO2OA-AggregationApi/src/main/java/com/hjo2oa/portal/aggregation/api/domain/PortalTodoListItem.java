package com.hjo2oa.portal.aggregation.api.domain;

import java.time.Instant;

public record PortalTodoListItem(
        String todoId,
        String taskId,
        String instanceId,
        String title,
        String category,
        String urgency,
        PortalTodoListViewType viewType,
        String status,
        Instant dueTime,
        Instant overdueAt,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        Instant readAt
) {
}
