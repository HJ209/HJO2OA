package com.hjo2oa.data.openapi.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ApiQuotaUsageCounter(
        String counterId,
        String tenantId,
        String policyId,
        String apiId,
        String clientCode,
        Instant windowStartedAt,
        long usedCount,
        Instant createdAt,
        Instant updatedAt
) {

    public ApiQuotaUsageCounter {
        counterId = requireText(counterId, "counterId");
        tenantId = requireText(tenantId, "tenantId");
        policyId = requireText(policyId, "policyId");
        apiId = requireText(apiId, "apiId");
        clientCode = requireText(clientCode, "clientCode");
        Objects.requireNonNull(windowStartedAt, "windowStartedAt must not be null");
        if (usedCount < 0) {
            throw new IllegalArgumentException("usedCount must not be negative");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ApiQuotaUsageCounter create(
            String tenantId,
            String policyId,
            String apiId,
            String clientCode,
            Instant windowStartedAt,
            Instant now
    ) {
        return new ApiQuotaUsageCounter(
                UUID.randomUUID().toString(),
                tenantId,
                policyId,
                apiId,
                clientCode,
                windowStartedAt,
                0L,
                now,
                now
        );
    }

    public ApiQuotaUsageCounter increment(Instant now) {
        return new ApiQuotaUsageCounter(
                counterId,
                tenantId,
                policyId,
                apiId,
                clientCode,
                windowStartedAt,
                usedCount + 1,
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
}
