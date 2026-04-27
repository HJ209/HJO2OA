package com.hjo2oa.process.monitor.domain;

import java.time.Instant;
import java.util.UUID;

public record NodeStagnationAnalysisView(
        UUID taskId,
        UUID instanceId,
        String instanceTitle,
        UUID definitionId,
        String definitionCode,
        String category,
        String nodeId,
        String nodeName,
        UUID assigneeId,
        String taskStatus,
        Instant taskCreatedAt,
        Instant dueTime,
        long stalledMinutes
) {
}
