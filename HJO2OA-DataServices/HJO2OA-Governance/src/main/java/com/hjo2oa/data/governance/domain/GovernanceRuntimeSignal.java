package com.hjo2oa.data.governance.domain;

import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.RuntimeTargetStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record GovernanceRuntimeSignal(
        String signalId,
        String tenantId,
        GovernanceScopeType targetType,
        String targetCode,
        RuntimeTargetStatus runtimeStatus,
        long totalExecutions,
        long failureCount,
        BigDecimal failureRate,
        Long lastDurationMs,
        Long freshnessLagSeconds,
        Instant lastSuccessAt,
        Instant lastFailureAt,
        String lastErrorCode,
        String lastErrorMessage,
        String lastEventType,
        String lastExecutionId,
        String traceId,
        String payloadJson,
        Instant updatedAt
) {

    public GovernanceRuntimeSignal {
        signalId = requireText(signalId, "signalId");
        tenantId = requireText(tenantId, "tenantId");
        Objects.requireNonNull(targetType, "targetType must not be null");
        targetCode = requireText(targetCode, "targetCode");
        Objects.requireNonNull(runtimeStatus, "runtimeStatus must not be null");
        if (totalExecutions < 0) {
            throw new IllegalArgumentException("totalExecutions must not be negative");
        }
        if (failureCount < 0) {
            throw new IllegalArgumentException("failureCount must not be negative");
        }
        Objects.requireNonNull(failureRate, "failureRate must not be null");
        lastErrorCode = normalizeOptional(lastErrorCode);
        lastErrorMessage = normalizeOptional(lastErrorMessage);
        lastEventType = normalizeOptional(lastEventType);
        lastExecutionId = normalizeOptional(lastExecutionId);
        traceId = normalizeOptional(traceId);
        payloadJson = normalizeOptional(payloadJson);
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static GovernanceRuntimeSignal initialize(
            String signalId,
            String tenantId,
            GovernanceScopeType targetType,
            String targetCode,
            Instant now
    ) {
        return new GovernanceRuntimeSignal(
                signalId,
                tenantId,
                targetType,
                targetCode,
                RuntimeTargetStatus.UNKNOWN,
                0,
                0,
                BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                now
        );
    }

    public GovernanceRuntimeSignal updateLifecycle(
            RuntimeTargetStatus runtimeStatus,
            String lastEventType,
            String payloadJson,
            Instant now
    ) {
        return new GovernanceRuntimeSignal(
                signalId,
                tenantId,
                targetType,
                targetCode,
                Objects.requireNonNull(runtimeStatus, "runtimeStatus must not be null"),
                totalExecutions,
                failureCount,
                failureRate,
                lastDurationMs,
                freshnessLagSeconds,
                lastSuccessAt,
                lastFailureAt,
                lastErrorCode,
                lastErrorMessage,
                lastEventType,
                lastExecutionId,
                traceId,
                payloadJson,
                now
        );
    }

    public GovernanceRuntimeSignal markSuccess(
            String lastEventType,
            String executionId,
            Long durationMs,
            Instant sourceOccurredAt,
            String payloadJson,
            Instant now
    ) {
        long nextTotal = totalExecutions + 1;
        return new GovernanceRuntimeSignal(
                signalId,
                tenantId,
                targetType,
                targetCode,
                RuntimeTargetStatus.ACTIVE,
                nextTotal,
                failureCount,
                calculateFailureRate(failureCount, nextTotal),
                durationMs,
                sourceOccurredAt == null ? freshnessLagSeconds : Duration.between(sourceOccurredAt, now).getSeconds(),
                now,
                lastFailureAt,
                null,
                null,
                lastEventType,
                normalizeOptional(executionId),
                null,
                payloadJson,
                now
        );
    }

    public GovernanceRuntimeSignal markFailure(
            String lastEventType,
            String executionId,
            String errorCode,
            String errorMessage,
            String traceId,
            String payloadJson,
            Instant now
    ) {
        long nextTotal = totalExecutions + 1;
        long nextFailureCount = failureCount + 1;
        return new GovernanceRuntimeSignal(
                signalId,
                tenantId,
                targetType,
                targetCode,
                RuntimeTargetStatus.FAILED,
                nextTotal,
                nextFailureCount,
                calculateFailureRate(nextFailureCount, nextTotal),
                lastDurationMs,
                freshnessLagSeconds,
                lastSuccessAt,
                now,
                errorCode,
                errorMessage,
                lastEventType,
                normalizeOptional(executionId),
                traceId,
                payloadJson,
                now
        );
    }

    public GovernanceRuntimeSignal updateFreshness(String lastEventType, Instant sourceOccurredAt, Instant now) {
        long lag = sourceOccurredAt == null ? 0 : Math.max(0, Duration.between(sourceOccurredAt, now).getSeconds());
        return new GovernanceRuntimeSignal(
                signalId,
                tenantId,
                targetType,
                targetCode,
                runtimeStatus,
                totalExecutions,
                failureCount,
                failureRate,
                lastDurationMs,
                lag,
                lastSuccessAt,
                lastFailureAt,
                lastErrorCode,
                lastErrorMessage,
                lastEventType,
                lastExecutionId,
                traceId,
                payloadJson,
                now
        );
    }

    private static BigDecimal calculateFailureRate(long failureCount, long totalExecutions) {
        if (totalExecutions <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(failureCount)
                .divide(BigDecimal.valueOf(totalExecutions), 4, RoundingMode.HALF_UP);
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
