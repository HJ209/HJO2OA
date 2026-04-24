package com.hjo2oa.data.common.infrastructure.audit;

import java.time.Instant;

public record DataAuditRecord(
        Instant timestamp,
        String module,
        String action,
        String targetType,
        String methodName,
        String tenantId,
        String operatorId,
        String requestId,
        boolean success,
        long durationMs,
        String errorCode,
        String message,
        String detail
) {
}
