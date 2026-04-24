package com.hjo2oa.data.openapi.domain;

import java.time.Instant;

public record ApiRateLimitPolicyView(
        String policyId,
        String policyCode,
        String clientCode,
        ApiPolicyType policyType,
        long windowValue,
        ApiWindowUnit windowUnit,
        long threshold,
        ApiPolicyStatus status,
        String description,
        long currentWindowUsedCount,
        Instant currentWindowStartedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
