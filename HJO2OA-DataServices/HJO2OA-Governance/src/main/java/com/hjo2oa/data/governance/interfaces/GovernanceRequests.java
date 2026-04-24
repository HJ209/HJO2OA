package com.hjo2oa.data.governance.interfaces;

import com.hjo2oa.data.governance.application.GovernanceCommands.AlertActionCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.DeprecateServiceVersionCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.ManualGovernanceInterventionCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.PublishServiceVersionCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.RegisterServiceVersionCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.RunHealthCheckCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertAlertRuleCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertGovernanceProfileCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertHealthCheckRuleCommand;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertLevel;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ComparisonOperator;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceActionType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceProfileStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckSeverity;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class GovernanceRequests {

    private GovernanceRequests() {
    }

    public record UpsertGovernanceProfileRequest(
            @NotBlank String tenantId,
            @NotBlank String code,
            @NotNull GovernanceScopeType scopeType,
            @NotBlank String targetCode,
            String slaPolicyJson,
            String alertPolicyJson,
            GovernanceProfileStatus status,
            String operatorId,
            String operatorName,
            String requestId
    ) {
        public UpsertGovernanceProfileCommand toCommand() {
            return new UpsertGovernanceProfileCommand(
                    tenantId,
                    code,
                    scopeType,
                    targetCode,
                    slaPolicyJson,
                    alertPolicyJson,
                    status,
                    operatorId,
                    operatorName,
                    requestId
            );
        }
    }

    public record UpsertHealthCheckRuleRequest(
            @NotBlank String tenantId,
            @NotBlank String ruleCode,
            @NotBlank String ruleName,
            @NotNull HealthCheckType checkType,
            @NotNull HealthCheckSeverity severity,
            @NotNull HealthCheckRuleStatus status,
            @NotBlank String metricName,
            @NotNull ComparisonOperator comparisonOperator,
            @NotNull BigDecimal thresholdValue,
            Integer windowMinutes,
            Integer dedupMinutes,
            String scheduleExpression,
            String strategyJson,
            String operatorId,
            String operatorName,
            String requestId
    ) {
        public UpsertHealthCheckRuleCommand toCommand(String profileCode) {
            return new UpsertHealthCheckRuleCommand(
                    tenantId,
                    profileCode,
                    ruleCode,
                    ruleName,
                    checkType,
                    severity,
                    status,
                    metricName,
                    comparisonOperator,
                    thresholdValue,
                    windowMinutes,
                    dedupMinutes,
                    scheduleExpression,
                    strategyJson,
                    operatorId,
                    operatorName,
                    requestId
            );
        }
    }

    public record UpsertAlertRuleRequest(
            @NotBlank String tenantId,
            @NotBlank String ruleCode,
            @NotBlank String ruleName,
            String sourceRuleCode,
            String metricName,
            @NotBlank String alertType,
            @NotNull AlertLevel alertLevel,
            @NotNull AlertRuleStatus status,
            @NotNull ComparisonOperator comparisonOperator,
            @NotNull BigDecimal thresholdValue,
            Integer dedupMinutes,
            Integer escalationMinutes,
            String notificationPolicyJson,
            String strategyJson,
            String operatorId,
            String operatorName,
            String requestId
    ) {
        public UpsertAlertRuleCommand toCommand(String profileCode) {
            return new UpsertAlertRuleCommand(
                    tenantId,
                    profileCode,
                    ruleCode,
                    ruleName,
                    sourceRuleCode,
                    metricName,
                    alertType,
                    alertLevel,
                    status,
                    comparisonOperator,
                    thresholdValue,
                    dedupMinutes,
                    escalationMinutes,
                    notificationPolicyJson,
                    strategyJson,
                    operatorId,
                    operatorName,
                    requestId
            );
        }
    }

    public record RegisterServiceVersionRequest(
            @NotBlank String tenantId,
            @NotBlank String profileCode,
            GovernanceScopeType targetType,
            String targetCode,
            @NotBlank String version,
            String compatibilityNote,
            String changeSummary,
            String approvalNote,
            String operatorId,
            String operatorName,
            String requestId
    ) {
        public RegisterServiceVersionCommand toCommand() {
            return new RegisterServiceVersionCommand(
                    tenantId,
                    profileCode,
                    targetType,
                    targetCode,
                    version,
                    compatibilityNote,
                    changeSummary,
                    approvalNote,
                    operatorId,
                    operatorName,
                    requestId
            );
        }
    }

    public record PublishServiceVersionRequest(
            @NotBlank String tenantId,
            @NotBlank String profileCode,
            @NotBlank String version,
            String approvalNote,
            String operatorId,
            String operatorName,
            String requestId
    ) {
        public PublishServiceVersionCommand toCommand() {
            return new PublishServiceVersionCommand(
                    tenantId,
                    profileCode,
                    version,
                    approvalNote,
                    operatorId,
                    operatorName,
                    requestId
            );
        }
    }

    public record DeprecateServiceVersionRequest(
            @NotBlank String tenantId,
            @NotBlank String profileCode,
            @NotBlank String version,
            String approvalNote,
            String operatorId,
            String operatorName,
            String requestId
    ) {
        public DeprecateServiceVersionCommand toCommand() {
            return new DeprecateServiceVersionCommand(
                    tenantId,
                    profileCode,
                    version,
                    approvalNote,
                    operatorId,
                    operatorName,
                    requestId
            );
        }
    }

    public record RunHealthCheckRequest(
            String tenantId,
            GovernanceScopeType targetType,
            String targetCode,
            String operatorId,
            String operatorName,
            String requestId
    ) {
        public RunHealthCheckCommand toCommand() {
            return new RunHealthCheckCommand(
                    tenantId,
                    targetType,
                    targetCode,
                    operatorId,
                    operatorName,
                    requestId
            );
        }
    }

    public record AlertActionRequest(
            @NotBlank String tenantId,
            String operatorId,
            String operatorName,
            String reason,
            String requestId
    ) {
        public AlertActionCommand toCommand(String alertId, GovernanceActionType actionType) {
            return new AlertActionCommand(
                    tenantId,
                    alertId,
                    actionType,
                    operatorId,
                    operatorName,
                    reason,
                    requestId
            );
        }
    }

    public record ManualGovernanceInterventionRequest(
            @NotBlank String tenantId,
            @NotNull GovernanceScopeType targetType,
            @NotBlank String targetCode,
            String traceId,
            @NotNull GovernanceActionType actionType,
            String operatorId,
            String operatorName,
            String reason,
            String payloadJson,
            String requestId
    ) {
        public ManualGovernanceInterventionCommand toCommand() {
            return new ManualGovernanceInterventionCommand(
                    tenantId,
                    targetType,
                    targetCode,
                    traceId,
                    actionType,
                    operatorId,
                    operatorName,
                    reason,
                    payloadJson,
                    requestId
            );
        }
    }
}
