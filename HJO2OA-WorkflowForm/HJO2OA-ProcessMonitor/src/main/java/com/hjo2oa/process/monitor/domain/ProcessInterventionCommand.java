package com.hjo2oa.process.monitor.domain;

import java.util.UUID;

public record ProcessInterventionCommand(
        UUID tenantId,
        UUID instanceId,
        UUID taskId,
        String actionType,
        UUID operatorId,
        UUID targetAssigneeId,
        String reason
) {
}
