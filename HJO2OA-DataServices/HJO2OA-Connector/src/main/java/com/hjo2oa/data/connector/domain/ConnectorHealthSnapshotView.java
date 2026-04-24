package com.hjo2oa.data.connector.domain;

import java.time.Instant;

public record ConnectorHealthSnapshotView(
        String snapshotId,
        String connectorId,
        ConnectorCheckType checkType,
        ConnectorHealthStatus healthStatus,
        long latencyMs,
        String errorCode,
        String errorSummary,
        String operatorId,
        String targetEnvironment,
        String confirmedBy,
        String confirmationNote,
        Instant confirmedAt,
        long changeSequence,
        Instant checkedAt
) {
}
