package com.hjo2oa.infra.security.domain;

import java.util.UUID;

public record RateLimitRuleView(
        UUID id,
        UUID securityPolicyId,
        RateLimitSubjectType subjectType,
        int windowSeconds,
        int maxRequests,
        boolean active
) {
}
