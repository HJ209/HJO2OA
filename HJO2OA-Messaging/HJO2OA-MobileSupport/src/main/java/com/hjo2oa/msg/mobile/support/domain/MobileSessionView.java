package com.hjo2oa.msg.mobile.support.domain;

import java.time.Instant;
import java.util.UUID;

public record MobileSessionView(
        UUID id,
        UUID deviceBindingId,
        UUID personId,
        UUID accountId,
        UUID currentAssignmentId,
        UUID currentPositionId,
        MobileSessionStatus sessionStatus,
        MobileRiskLevel riskLevelSnapshot,
        Instant riskFrozenAt,
        String riskReason,
        Instant issuedAt,
        Instant expiresAt,
        Instant lastHeartbeatAt,
        int refreshVersion,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
