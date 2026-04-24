package com.hjo2oa.data.connector.domain;

import java.time.Instant;

public record ConnectorSummaryView(
        String connectorId,
        String tenantId,
        String code,
        String name,
        ConnectorType connectorType,
        String vendor,
        String protocol,
        ConnectorAuthMode authMode,
        TimeoutRetryConfig timeoutConfig,
        ConnectorStatus status,
        long changeSequence,
        ConnectorHealthSnapshotView latestTestSnapshot,
        ConnectorHealthSnapshotView latestHealthSnapshot,
        Instant createdAt,
        Instant updatedAt
) {
}
