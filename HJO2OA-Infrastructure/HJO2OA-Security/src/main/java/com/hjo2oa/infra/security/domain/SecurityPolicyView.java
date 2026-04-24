package com.hjo2oa.infra.security.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SecurityPolicyView(
        UUID id,
        String policyCode,
        SecurityPolicyType policyType,
        String name,
        SecurityPolicyStatus status,
        UUID tenantId,
        String configSnapshot,
        Instant createdAt,
        Instant updatedAt,
        List<SecretKeyMaterialView> secretKeys,
        List<MaskingRuleView> maskingRules,
        List<RateLimitRuleView> rateLimitRules
) {

    public SecurityPolicyView {
        secretKeys = List.copyOf(secretKeys);
        maskingRules = List.copyOf(maskingRules);
        rateLimitRules = List.copyOf(rateLimitRules);
    }
}
