package com.hjo2oa.process.monitor.domain;

import java.time.Instant;
import java.util.UUID;

public record OverdueTaskObservationView(
        UUID taskId,
        UUID instanceId,
        String instanceTitle,
        UUID definitionId,
        String definitionCode,
        String category,
        String nodeId,
        String nodeName,
        UUID assigneeId,
        Instant dueTime,
        long overdueMinutes
) {
}
