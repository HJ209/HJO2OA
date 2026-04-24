package com.hjo2oa.infra.security.interfaces;

import com.hjo2oa.infra.security.domain.MaskingRuleView;
import com.hjo2oa.infra.security.domain.RateLimitRuleView;
import com.hjo2oa.infra.security.domain.SecretKeyMaterialView;
import com.hjo2oa.infra.security.domain.SecurityPolicyView;
import org.springframework.stereotype.Component;

@Component
public class SecurityPolicyDtoMapper {

    public SecurityPolicyDtos.SecurityPolicyResponse toResponse(SecurityPolicyView view) {
        return new SecurityPolicyDtos.SecurityPolicyResponse(
                view.id(),
                view.policyCode(),
                view.policyType(),
                view.name(),
                view.status(),
                view.tenantId(),
                view.configSnapshot(),
                view.createdAt(),
                view.updatedAt(),
                view.secretKeys().stream().map(this::toResponse).toList(),
                view.maskingRules().stream().map(this::toResponse).toList(),
                view.rateLimitRules().stream().map(this::toResponse).toList()
        );
    }

    public SecurityPolicyDtos.MaskValueResponse toMaskValueResponse(String maskedValue) {
        return new SecurityPolicyDtos.MaskValueResponse(maskedValue);
    }

    private SecurityPolicyDtos.SecretKeyMaterialResponse toResponse(SecretKeyMaterialView view) {
        return new SecurityPolicyDtos.SecretKeyMaterialResponse(
                view.id(),
                view.securityPolicyId(),
                view.keyRef(),
                view.algorithm(),
                view.keyStatus(),
                view.rotatedAt()
        );
    }

    private SecurityPolicyDtos.MaskingRuleResponse toResponse(MaskingRuleView view) {
        return new SecurityPolicyDtos.MaskingRuleResponse(
                view.id(),
                view.securityPolicyId(),
                view.dataType(),
                view.ruleExpr(),
                view.active()
        );
    }

    private SecurityPolicyDtos.RateLimitRuleResponse toResponse(RateLimitRuleView view) {
        return new SecurityPolicyDtos.RateLimitRuleResponse(
                view.id(),
                view.securityPolicyId(),
                view.subjectType(),
                view.windowSeconds(),
                view.maxRequests(),
                view.active()
        );
    }
}
