package com.hjo2oa.data.governance.domain;

public final class GovernanceTypes {

    private GovernanceTypes() {
    }

    public enum GovernanceScopeType {
        API,
        CONNECTOR,
        SYNC,
        REPORT,
        MODULE
    }

    public enum GovernanceProfileStatus {
        ACTIVE,
        DISABLED
    }

    public enum HealthCheckRuleStatus {
        ENABLED,
        DISABLED
    }

    public enum HealthCheckSeverity {
        INFO,
        WARN,
        ERROR,
        CRITICAL
    }

    public enum HealthCheckType {
        HEARTBEAT,
        FRESHNESS,
        FAILURE_RATE,
        VERSION_DRIFT,
        CUSTOM
    }

    public enum ComparisonOperator {
        GREATER_THAN,
        GREATER_OR_EQUAL,
        LESS_THAN,
        LESS_OR_EQUAL,
        EQUAL
    }

    public enum AlertRuleStatus {
        ENABLED,
        DISABLED
    }

    public enum AlertLevel {
        INFO,
        WARN,
        ERROR,
        CRITICAL
    }

    public enum AlertStatus {
        OPEN,
        ACKNOWLEDGED,
        ESCALATED,
        CLOSED
    }

    public enum ServiceVersionStatus {
        REGISTERED,
        PUBLISHED,
        DEPRECATED,
        ROLLED_BACK
    }

    public enum GovernanceActionType {
        UPSERT_PROFILE,
        UPSERT_HEALTH_RULE,
        UPSERT_ALERT_RULE,
        REGISTER_VERSION,
        PUBLISH_VERSION,
        DEPRECATE_VERSION,
        RUN_HEALTH_CHECK,
        ACKNOWLEDGE_ALERT,
        ESCALATE_ALERT,
        CLOSE_ALERT,
        REQUEST_DISABLE,
        REQUEST_RETRY,
        REQUEST_DEGRADE,
        REQUEST_COMPENSATION,
        ADD_NOTE
    }

    public enum GovernanceActionResult {
        ACCEPTED,
        REJECTED,
        COMPLETED
    }

    public enum TraceStatus {
        OPEN,
        INVESTIGATING,
        COMPENSATED,
        RESOLVED
    }

    public enum GovernanceTraceType {
        HEALTH_CHECK,
        ALERT,
        SYNC_FAILURE,
        PUBLICATION,
        MANUAL_COMPENSATION
    }

    public enum RuntimeTargetStatus {
        UNKNOWN,
        ACTIVE,
        DEGRADED,
        FAILED,
        DISABLED
    }

    public enum GovernanceHealthStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        UNKNOWN
    }
}
