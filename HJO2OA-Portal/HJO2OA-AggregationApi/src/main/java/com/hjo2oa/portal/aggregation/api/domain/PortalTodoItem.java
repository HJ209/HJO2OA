package com.hjo2oa.portal.aggregation.api.domain;

import java.time.Instant;

public record PortalTodoItem(
        String todoId,
        String title,
        String category,
        String urgency,
        Instant dueTime,
        Instant createdAt
) {
}
