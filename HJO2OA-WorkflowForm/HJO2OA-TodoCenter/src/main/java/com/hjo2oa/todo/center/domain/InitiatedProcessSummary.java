package com.hjo2oa.todo.center.domain;

import java.time.Instant;
import java.util.UUID;

public record InitiatedProcessSummary(
        UUID instanceId,
        UUID definitionId,
        String definitionCode,
        String title,
        String category,
        String status,
        Instant startTime,
        Instant endTime,
        Instant updatedAt
) {
}
