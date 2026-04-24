package com.hjo2oa.data.governance.domain;

import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceActionResult;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceActionType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import java.time.Instant;
import java.util.Objects;

public record GovernanceActionAuditRecord(
        String auditId,
        String governanceId,
        GovernanceScopeType targetType,
        String targetCode,
        GovernanceActionType actionType,
        GovernanceActionResult actionResult,
        String operatorId,
        String operatorName,
        String reason,
        String requestId,
        String payloadJson,
        String resultMessage,
        String traceId,
        Instant createdAt,
        Instant completedAt
) {

    public GovernanceActionAuditRecord {
        auditId = requireText(auditId, "auditId");
        governanceId = requireText(governanceId, "governanceId");
        Objects.requireNonNull(targetType, "targetType must not be null");
        targetCode = requireText(targetCode, "targetCode");
        Objects.requireNonNull(actionType, "actionType must not be null");
        Objects.requireNonNull(actionResult, "actionResult must not be null");
        operatorId = requireText(operatorId, "operatorId");
        operatorName = normalizeOptional(operatorName);
        reason = normalizeOptional(reason);
        requestId = requireText(requestId, "requestId");
        payloadJson = normalizeOptional(payloadJson);
        resultMessage = normalizeOptional(resultMessage);
        traceId = normalizeOptional(traceId);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
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
