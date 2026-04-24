package com.hjo2oa.data.governance.domain;

import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceTraceType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.TraceStatus;
import java.time.Instant;
import java.util.Objects;

public record GovernanceTraceRecord(
        String traceId,
        String governanceId,
        GovernanceScopeType targetType,
        String targetCode,
        GovernanceTraceType traceType,
        TraceStatus status,
        String sourceEventType,
        String sourceExecutionId,
        String correlationId,
        String summary,
        String detail,
        Instant openedAt,
        Instant updatedAt,
        Instant resolvedAt
) {

    public GovernanceTraceRecord {
        traceId = requireText(traceId, "traceId");
        governanceId = requireText(governanceId, "governanceId");
        Objects.requireNonNull(targetType, "targetType must not be null");
        targetCode = requireText(targetCode, "targetCode");
        Objects.requireNonNull(traceType, "traceType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        sourceEventType = normalizeOptional(sourceEventType);
        sourceExecutionId = normalizeOptional(sourceExecutionId);
        correlationId = normalizeOptional(correlationId);
        summary = requireText(summary, "summary");
        detail = normalizeOptional(detail);
        Objects.requireNonNull(openedAt, "openedAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public GovernanceTraceRecord updateStatus(
            TraceStatus status,
            String summary,
            String detail,
            Instant updatedAt,
            Instant resolvedAt
    ) {
        return new GovernanceTraceRecord(
                traceId,
                governanceId,
                targetType,
                targetCode,
                traceType,
                status,
                sourceEventType,
                sourceExecutionId,
                correlationId,
                summary,
                detail,
                openedAt,
                updatedAt,
                resolvedAt
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
