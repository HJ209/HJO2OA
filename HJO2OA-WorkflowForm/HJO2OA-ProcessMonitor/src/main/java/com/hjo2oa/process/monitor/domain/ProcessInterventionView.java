package com.hjo2oa.process.monitor.domain;

import java.time.Instant;
import java.util.UUID;

public record ProcessInterventionView(
        UUID interventionId,
        UUID instanceId,
        UUID taskId,
        String actionType,
        UUID operatorId,
        UUID targetAssigneeId,
        String reason,
        Instant createdAt
) {
}
