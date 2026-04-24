package com.hjo2oa.data.connector.domain;

public record ConnectorHealthOverviewView(
        String connectorId,
        ConnectorHealthSnapshotView latestHealthSnapshot,
        ConnectorHealthSnapshotView lastFailureSnapshot,
        int sampleSize,
        long healthyCount,
        long degradedCount,
        long unreachableCount
) {
}
