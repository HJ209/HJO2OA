package com.hjo2oa.data.governance.domain;

import com.hjo2oa.data.governance.domain.GovernanceTypes.ComparisonOperator;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckSeverity;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record HealthCheckRule(
        String ruleId,
        String governanceId,
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
        Instant createdAt,
        Instant updatedAt
) {

    public HealthCheckRule {
        ruleId = requireText(ruleId, "ruleId");
        governanceId = requireText(governanceId, "governanceId");
        ruleCode = requireText(ruleCode, "ruleCode");
        ruleName = requireText(ruleName, "ruleName");
        Objects.requireNonNull(checkType, "checkType must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(status, "status must not be null");
        metricName = requireText(metricName, "metricName");
        Objects.requireNonNull(comparisonOperator, "comparisonOperator must not be null");
        Objects.requireNonNull(thresholdValue, "thresholdValue must not be null");
        if (windowMinutes != null && windowMinutes < 0) {
            throw new IllegalArgumentException("windowMinutes must not be negative");
        }
        if (dedupMinutes != null && dedupMinutes < 0) {
            throw new IllegalArgumentException("dedupMinutes must not be negative");
        }
        scheduleExpression = normalizeOptional(scheduleExpression);
        strategyJson = normalizeOptional(strategyJson);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public HealthCheckRule update(
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
            Instant now
    ) {
        return new HealthCheckRule(
                ruleId,
                governanceId,
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
                createdAt,
                now
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
