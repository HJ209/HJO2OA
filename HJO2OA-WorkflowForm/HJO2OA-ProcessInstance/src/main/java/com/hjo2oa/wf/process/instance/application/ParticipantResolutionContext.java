package com.hjo2oa.wf.process.instance.application;

import java.util.Map;
import java.util.UUID;

public record ParticipantResolutionContext(
        UUID tenantId,
        UUID initiatorId,
        UUID initiatorOrgId,
        UUID initiatorDeptId,
        UUID initiatorPositionId,
        Map<String, Object> variables
) {
}
