package com.hjo2oa.data.openapi.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ApiInvocationAuditLog(
        String logId,
        String requestId,
        String tenantId,
        String apiId,
        String endpointCode,
        String endpointVersion,
        String path,
        OpenApiHttpMethod httpMethod,
        String clientCode,
        OpenApiAuthType authType,
        ApiInvocationOutcome outcome,
        int responseStatus,
        String errorCode,
        long durationMs,
        String requestDigest,
        String remoteIp,
        Instant occurredAt,
        boolean abnormalFlag,
        String reviewConclusion,
        String note,
        String reviewedBy,
        Instant reviewedAt
) {

    public ApiInvocationAuditLog {
        logId = requireText(logId, "logId");
        requestId = requireText(requestId, "requestId");
        tenantId = requireText(tenantId, "tenantId");
        apiId = normalizeNullable(apiId);
        endpointCode = normalizeNullable(endpointCode);
        endpointVersion = normalizeNullable(endpointVersion);
        path = requireText(path, "path");
        Objects.requireNonNull(httpMethod, "httpMethod must not be null");
        clientCode = normalizeNullable(clientCode);
        authType = authType;
        Objects.requireNonNull(outcome, "outcome must not be null");
        if (responseStatus < 100) {
            throw new IllegalArgumentException("responseStatus must be a valid http status");
        }
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs must not be negative");
        }
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static ApiInvocationAuditLog create(
            String requestId,
            String tenantId,
            String apiId,
            String endpointCode,
            String endpointVersion,
            String path,
            OpenApiHttpMethod httpMethod,
            String clientCode,
            OpenApiAuthType authType,
            ApiInvocationOutcome outcome,
            int responseStatus,
            String errorCode,
            long durationMs,
            String requestDigest,
            String remoteIp,
            Instant occurredAt
    ) {
        return new ApiInvocationAuditLog(
                UUID.randomUUID().toString(),
                requestId,
                tenantId,
                apiId,
                endpointCode,
                endpointVersion,
                path,
                httpMethod,
                clientCode,
                authType,
                outcome,
                responseStatus,
                errorCode,
                durationMs,
                requestDigest,
                remoteIp,
                occurredAt,
                false,
                null,
                null,
                null,
                null
        );
    }

    public ApiInvocationAuditLog review(boolean abnormalFlag, String reviewConclusion, String note, String reviewedBy, Instant reviewedAt) {
        return new ApiInvocationAuditLog(
                logId,
                requestId,
                tenantId,
                apiId,
                endpointCode,
                endpointVersion,
                path,
                httpMethod,
                clientCode,
                authType,
                outcome,
                responseStatus,
                errorCode,
                durationMs,
                requestDigest,
                remoteIp,
                occurredAt,
                abnormalFlag,
                normalizeNullable(reviewConclusion),
                normalizeNullable(note),
                requireText(reviewedBy, "reviewedBy"),
                Objects.requireNonNull(reviewedAt, "reviewedAt must not be null")
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

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
