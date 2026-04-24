package com.hjo2oa.data.connector.domain;

public record ConnectorTestResult(
        ConnectorHealthStatus healthStatus,
        long latencyMs,
        String errorCode,
        String errorSummary
) {

    public static ConnectorTestResult healthy(long latencyMs) {
        return new ConnectorTestResult(ConnectorHealthStatus.HEALTHY, latencyMs, null, null);
    }

    public static ConnectorTestResult unhealthy(long latencyMs, ConnectorFailureReason failureReason, String errorSummary) {
        return new ConnectorTestResult(
                resolveHealthStatus(failureReason),
                latencyMs,
                failureReason == null ? ConnectorFailureReason.UNKNOWN.name() : failureReason.name(),
                errorSummary
        );
    }

    public boolean healthy() {
        return healthStatus == ConnectorHealthStatus.HEALTHY;
    }

    private static ConnectorHealthStatus resolveHealthStatus(ConnectorFailureReason failureReason) {
        if (failureReason == ConnectorFailureReason.NETWORK_UNREACHABLE) {
            return ConnectorHealthStatus.UNREACHABLE;
        }
        return ConnectorHealthStatus.DEGRADED;
    }
}
