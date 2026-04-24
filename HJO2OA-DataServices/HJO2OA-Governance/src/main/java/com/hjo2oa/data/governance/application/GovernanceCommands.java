package com.hjo2oa.data.governance.application;

import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertLevel;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ComparisonOperator;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceActionType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceProfileStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckSeverity;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ServiceVersionStatus;
import java.math.BigDecimal;
import java.util.List;

public final class GovernanceCommands {

    private GovernanceCommands() {
    }

    public record UpsertGovernanceProfileCommand(
            String tenantId,
            String code,
            GovernanceScopeType scopeType,
            String targetCode,
            String slaPolicyJson,
            String alertPolicyJson,
            GovernanceProfileStatus status,
            String operatorId,
            String operatorName,
            String requestId
    ) {
    }

    public record UpsertHealthCheckRuleCommand(
            String tenantId,
            String profileCode,
            String ruleCode,
            String ruleName,
            HealthCheckType checkType,
            HealthCheckSeverity severity,
            HealthCheckRuleStatus status,
            String metricName,
            ComparisonOperator comparisonOperator,
            BigDecimal thresholdValue,
            Integer windowMinutes,
            Integer dedupMinutes,
            String scheduleExpression,
            String strategyJson,
            String operatorId,
            String operatorName,
            String requestId
    ) {
    }

    public record UpsertAlertRuleCommand(
            String tenantId,
            String profileCode,
            String ruleCode,
            String ruleName,
            String sourceRuleCode,
            String metricName,
            String alertType,
            AlertLevel alertLevel,
            AlertRuleStatus status,
            ComparisonOperator comparisonOperator,
            BigDecimal thresholdValue,
            Integer dedupMinutes,
            Integer escalationMinutes,
            String notificationPolicyJson,
            String strategyJson,
            String operatorId,
            String operatorName,
            String requestId
    ) {
    }

    public record RegisterServiceVersionCommand(
            String tenantId,
            String profileCode,
            GovernanceScopeType targetType,
            String targetCode,
            String version,
            String compatibilityNote,
            String changeSummary,
            String approvalNote,
            String operatorId,
            String operatorName,
            String requestId
    ) {
    }

    public record PublishServiceVersionCommand(
            String tenantId,
            String profileCode,
            String version,
            String approvalNote,
            String operatorId,
            String operatorName,
            String requestId
    ) {
    }

    public record DeprecateServiceVersionCommand(
            String tenantId,
            String profileCode,
            String version,
            String approvalNote,
            String operatorId,
            String operatorName,
            String requestId
    ) {
    }

    public record RunHealthCheckCommand(
            String tenantId,
            GovernanceScopeType targetType,
            String targetCode,
            String operatorId,
            String operatorName,
            String requestId
    ) {
    }

    public record AlertActionCommand(
            String tenantId,
            String alertId,
            GovernanceActionType actionType,
            String operatorId,
            String operatorName,
            String reason,
            String requestId
    ) {
    }

    public record ManualGovernanceInterventionCommand(
            String tenantId,
            GovernanceScopeType targetType,
            String targetCode,
            String traceId,
            GovernanceActionType actionType,
            String operatorId,
            String operatorName,
            String reason,
            String payloadJson,
            String requestId
    ) {
    }

    public record GovernancePagedResult<T>(List<T> items, long total) {

        public GovernancePagedResult {
            items = List.copyOf(items);
        }
    }

    public record VersionListQuery(
            String tenantId,
            GovernanceScopeType targetType,
            String targetCode,
            ServiceVersionStatus status
    ) {
    }
}
