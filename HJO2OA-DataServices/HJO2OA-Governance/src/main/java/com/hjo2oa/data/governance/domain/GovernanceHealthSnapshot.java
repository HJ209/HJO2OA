package com.hjo2oa.data.governance.domain;

import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceHealthStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record GovernanceHealthSnapshot(
        String snapshotId,
        String governanceId,
        String ruleId,
        GovernanceScopeType targetType,
        String targetCode,
        String ruleCode,
        GovernanceHealthStatus healthStatus,
        BigDecimal measuredValue,
        BigDecimal thresholdValue,
        String summary,
        String traceId,
        Instant checkedAt
) {

    public GovernanceHealthSnapshot {
        snapshotId = requireText(snapshotId, "snapshotId");
        governanceId = requireText(governanceId, "governanceId");
        ruleId = requireText(ruleId, "ruleId");
        Objects.requireNonNull(targetType, "targetType must not be null");
        targetCode = requireText(targetCode, "targetCode");
        ruleCode = requireText(ruleCode, "ruleCode");
        Objects.requireNonNull(healthStatus, "healthStatus must not be null");
        measuredValue = measuredValue == null ? BigDecimal.ZERO : measuredValue;
        thresholdValue = thresholdValue == null ? BigDecimal.ZERO : thresholdValue;
        summary = requireText(summary, "summary");
        traceId = normalizeOptional(traceId);
        Objects.requireNonNull(checkedAt, "checkedAt must not be null");
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
