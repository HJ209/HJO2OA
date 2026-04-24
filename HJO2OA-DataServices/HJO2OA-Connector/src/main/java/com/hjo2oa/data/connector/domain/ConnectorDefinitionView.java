package com.hjo2oa.data.connector.domain;

import java.time.Instant;
import java.util.List;

public record ConnectorDefinitionView(
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
        List<ConnectorParameterView> parameters,
        ConnectorHealthSnapshotView latestTestSnapshot,
        ConnectorHealthSnapshotView latestHealthSnapshot,
        Instant createdAt,
        Instant updatedAt
) {

    public ConnectorDefinitionView {
        parameters = List.copyOf(parameters);
    }
}
