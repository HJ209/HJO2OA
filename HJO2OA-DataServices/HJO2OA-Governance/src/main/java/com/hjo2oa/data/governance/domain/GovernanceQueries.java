package com.hjo2oa.data.governance.domain;

import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertLevel;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.TraceStatus;
import java.time.Instant;

public final class GovernanceQueries {

    private GovernanceQueries() {
    }

    public record AlertQuery(
            GovernanceScopeType targetType,
            String targetCode,
            AlertLevel alertLevel,
            AlertStatus alertStatus,
            Instant occurredFrom,
            Instant occurredTo
    ) {
    }

    public record TraceQuery(
            GovernanceScopeType targetType,
            String targetCode,
            TraceStatus traceStatus,
            Instant openedFrom,
            Instant openedTo
    ) {
    }

    public record AuditQuery(
            GovernanceScopeType targetType,
            String targetCode,
            Instant createdFrom,
            Instant createdTo
    ) {
    }

    public record HealthSnapshotQuery(
            GovernanceScopeType targetType,
            String targetCode,
            String ruleCode,
            Instant checkedFrom,
            Instant checkedTo
    ) {
    }
}
