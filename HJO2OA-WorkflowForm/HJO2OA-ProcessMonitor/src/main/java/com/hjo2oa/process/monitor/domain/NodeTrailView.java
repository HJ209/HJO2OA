package com.hjo2oa.process.monitor.domain;

import java.time.Instant;
import java.util.UUID;

public record NodeTrailView(
        UUID taskId,
        UUID instanceId,
        String nodeId,
        String nodeName,
        String nodeType,
        UUID assigneeId,
        String taskStatus,
        Instant createdAt,
        Instant claimTime,
        Instant completedTime,
        Instant dueTime,
        String lastActionCode,
        String lastActionName,
        UUID lastOperatorId,
        Instant lastActionAt
) {
}
