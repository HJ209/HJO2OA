package com.hjo2oa.todo.center.domain;

import java.time.Instant;

public record TodoActionLog(
        String idempotencyKey,
        String actionType,
        String targetId,
        Instant processedAt
) {
}
