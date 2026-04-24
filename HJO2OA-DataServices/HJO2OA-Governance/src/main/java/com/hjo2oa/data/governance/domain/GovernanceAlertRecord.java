package com.hjo2oa.data.governance.domain;

import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertLevel;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import java.time.Instant;
import java.util.Objects;

public record GovernanceAlertRecord(
        String alertId,
        String governanceId,
        String ruleId,
        GovernanceScopeType targetType,
        String targetCode,
        AlertLevel alertLevel,
        String alertType,
        AlertStatus status,
        String alertKey,
        String summary,
        String detail,
        String traceId,
        Instant occurredAt,
        Instant acknowledgedAt,
        String acknowledgedBy,
        Instant escalatedAt,
        String escalatedBy,
        Instant closedAt,
        String closedBy,
        String closeReason
) {

    public GovernanceAlertRecord {
        alertId = requireText(alertId, "alertId");
        governanceId = requireText(governanceId, "governanceId");
        ruleId = requireText(ruleId, "ruleId");
        Objects.requireNonNull(targetType, "targetType must not be null");
        targetCode = requireText(targetCode, "targetCode");
        Objects.requireNonNull(alertLevel, "alertLevel must not be null");
        alertType = requireText(alertType, "alertType");
        Objects.requireNonNull(status, "status must not be null");
        alertKey = requireText(alertKey, "alertKey");
        summary = requireText(summary, "summary");
        detail = normalizeOptional(detail);
        traceId = normalizeOptional(traceId);
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        acknowledgedBy = normalizeOptional(acknowledgedBy);
        escalatedBy = normalizeOptional(escalatedBy);
        closedBy = normalizeOptional(closedBy);
        closeReason = normalizeOptional(closeReason);
    }

    public GovernanceAlertRecord acknowledge(String operatorId, Instant now) {
        return new GovernanceAlertRecord(
                alertId,
                governanceId,
                ruleId,
                targetType,
                targetCode,
                alertLevel,
                alertType,
                AlertStatus.ACKNOWLEDGED,
                alertKey,
                summary,
                detail,
                traceId,
                occurredAt,
                now,
                operatorId,
                escalatedAt,
                escalatedBy,
                closedAt,
                closedBy,
                closeReason
        );
    }

    public GovernanceAlertRecord escalate(String operatorId, Instant now) {
        return new GovernanceAlertRecord(
                alertId,
                governanceId,
                ruleId,
                targetType,
                targetCode,
                alertLevel,
                alertType,
                AlertStatus.ESCALATED,
                alertKey,
                summary,
                detail,
                traceId,
                occurredAt,
                acknowledgedAt,
                acknowledgedBy,
                now,
                operatorId,
                closedAt,
                closedBy,
                closeReason
        );
    }

    public GovernanceAlertRecord close(String operatorId, String reason, Instant now) {
        return new GovernanceAlertRecord(
                alertId,
                governanceId,
                ruleId,
                targetType,
                targetCode,
                alertLevel,
                alertType,
                AlertStatus.CLOSED,
                alertKey,
                summary,
                detail,
                traceId,
                occurredAt,
                acknowledgedAt,
                acknowledgedBy,
                escalatedAt,
                escalatedBy,
                now,
                operatorId,
                reason
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
