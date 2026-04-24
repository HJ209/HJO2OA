package com.hjo2oa.data.governance.domain;

import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertLevel;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ComparisonOperator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record AlertRule(
        String ruleId,
        String governanceId,
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
        Instant createdAt,
        Instant updatedAt
) {

    public AlertRule {
        ruleId = requireText(ruleId, "ruleId");
        governanceId = requireText(governanceId, "governanceId");
        ruleCode = requireText(ruleCode, "ruleCode");
        ruleName = requireText(ruleName, "ruleName");
        sourceRuleCode = normalizeOptional(sourceRuleCode);
        metricName = normalizeOptional(metricName);
        alertType = requireText(alertType, "alertType");
        Objects.requireNonNull(alertLevel, "alertLevel must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(comparisonOperator, "comparisonOperator must not be null");
        Objects.requireNonNull(thresholdValue, "thresholdValue must not be null");
        if (dedupMinutes != null && dedupMinutes < 0) {
            throw new IllegalArgumentException("dedupMinutes must not be negative");
        }
        if (escalationMinutes != null && escalationMinutes < 0) {
            throw new IllegalArgumentException("escalationMinutes must not be negative");
        }
        notificationPolicyJson = normalizeOptional(notificationPolicyJson);
        strategyJson = normalizeOptional(strategyJson);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public AlertRule update(
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
            Instant now
    ) {
        return new AlertRule(
                ruleId,
                governanceId,
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
