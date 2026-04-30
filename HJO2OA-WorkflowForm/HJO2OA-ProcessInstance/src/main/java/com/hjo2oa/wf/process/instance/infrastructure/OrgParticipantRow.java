package com.hjo2oa.wf.process.instance.infrastructure;

import java.util.UUID;

public record OrgParticipantRow(
        UUID personId,
        UUID orgId,
        UUID deptId,
        UUID positionId
) {
}
