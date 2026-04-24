package com.hjo2oa.infra.security.domain;

import java.util.Objects;
import java.util.UUID;

public record RateLimitRule(
        UUID id,
        UUID securityPolicyId,
        RateLimitSubjectType subjectType,
        int windowSeconds,
        int maxRequests,
        boolean active
) {

    public RateLimitRule {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(securityPolicyId, "securityPolicyId must not be null");
        Objects.requireNonNull(subjectType, "subjectType must not be null");
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be greater than 0");
        }
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be greater than 0");
        }
    }

    public static RateLimitRule create(
            UUID id,
            UUID securityPolicyId,
            RateLimitSubjectType subjectType,
            int windowSeconds,
            int maxRequests
    ) {
        return new RateLimitRule(id, securityPolicyId, subjectType, windowSeconds, maxRequests, true);
    }

    public RateLimitRuleView toView() {
        return new RateLimitRuleView(id, securityPolicyId, subjectType, windowSeconds, maxRequests, active);
    }
}
