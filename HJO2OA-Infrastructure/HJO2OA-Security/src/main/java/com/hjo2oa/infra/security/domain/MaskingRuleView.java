package com.hjo2oa.infra.security.domain;

import java.util.UUID;

public record MaskingRuleView(
        UUID id,
        UUID securityPolicyId,
        String dataType,
        String ruleExpr,
        boolean active
) {
}
