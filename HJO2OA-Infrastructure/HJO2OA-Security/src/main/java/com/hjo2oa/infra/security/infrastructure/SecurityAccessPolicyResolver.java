package com.hjo2oa.infra.security.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.infra.security.domain.SecurityPolicy;
import com.hjo2oa.infra.security.domain.SecurityPolicyRepository;
import com.hjo2oa.infra.security.domain.SecurityPolicyStatus;
import com.hjo2oa.infra.security.domain.SecurityPolicyType;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class SecurityAccessPolicyResolver {

    private final SecurityPolicyRepository securityPolicyRepository;
    private final ObjectMapper objectMapper;

    SecurityAccessPolicyResolver(SecurityPolicyRepository securityPolicyRepository, ObjectMapper objectMapper) {
        this.securityPolicyRepository = Objects.requireNonNull(
                securityPolicyRepository,
                "securityPolicyRepository must not be null"
        );
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    List<SecurityAccessPolicy> resolve() {
        UUID currentTenantId = TenantContextHolder.currentTenantId().orElse(null);
        return securityPolicyRepository.findAll().stream()
                .filter(policy -> policy.policyType() == SecurityPolicyType.ACCESS_CONTROL)
                .filter(policy -> policy.status() == SecurityPolicyStatus.ACTIVE)
                .filter(policy -> policy.tenantId() == null || policy.tenantId().equals(currentTenantId))
                .map(this::toAccessPolicy)
                .toList();
    }

    private SecurityAccessPolicy toAccessPolicy(SecurityPolicy policy) {
        try {
            JsonNode root = objectMapper.readTree(policy.configSnapshot());
            return new SecurityAccessPolicy(
                    readStringArray(root.path("paths")),
                    readStringArray(root.path("ipWhitelist")),
                    policy.rateLimitRules().stream().filter(rule -> rule.active()).toList()
            );
        } catch (Exception ex) {
            return new SecurityAccessPolicy(List.of(), List.of(), policy.rateLimitRules());
        }
    }

    private List<String> readStringArray(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            if (value.isTextual() && !value.textValue().isBlank()) {
                values.add(value.textValue().trim());
            }
        });
        return values;
    }
}
