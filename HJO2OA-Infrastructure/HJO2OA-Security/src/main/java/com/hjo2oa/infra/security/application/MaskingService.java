package com.hjo2oa.infra.security.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hjo2oa.infra.security.domain.MaskingRule;
import com.hjo2oa.infra.security.domain.SecurityPolicy;
import com.hjo2oa.infra.security.domain.SecurityPolicyRepository;
import com.hjo2oa.infra.security.domain.SecurityPolicyStatus;
import com.hjo2oa.infra.security.domain.SecurityPolicyType;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MaskingService {

    private final SecurityPolicyRepository securityPolicyRepository;
    private final ObjectMapper objectMapper;

    public MaskingService(SecurityPolicyRepository securityPolicyRepository, ObjectMapper objectMapper) {
        this.securityPolicyRepository = Objects.requireNonNull(
                securityPolicyRepository,
                "securityPolicyRepository must not be null"
        );
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public String mask(String policyCode, String dataType, String value) {
        if (value == null) {
            return null;
        }
        return activeMaskingPolicies().stream()
                .filter(policy -> policyCode == null || policy.policyCode().equalsIgnoreCase(policyCode))
                .flatMap(policy -> policy.maskingRules().stream())
                .filter(rule -> rule.matches(dataType))
                .findFirst()
                .map(rule -> rule.mask(value))
                .orElse(value);
    }

    public Object maskResponseData(Object data) {
        if (data == null) {
            return null;
        }
        List<MaskingRule> rules = activeMaskingPolicies().stream()
                .flatMap(policy -> policy.maskingRules().stream())
                .filter(MaskingRule::active)
                .toList();
        if (rules.isEmpty()) {
            return data;
        }
        JsonNode root = objectMapper.valueToTree(data);
        maskNode(root, rules);
        return root;
    }

    private void maskNode(JsonNode node, List<MaskingRule> rules) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode child = field.getValue();
                if (child != null && child.isTextual()) {
                    rules.stream()
                            .filter(rule -> matchesField(rule, field.getKey()))
                            .findFirst()
                            .ifPresent(rule -> objectNode.put(field.getKey(), rule.mask(child.textValue())));
                } else {
                    maskNode(child, rules);
                }
            }
            return;
        }
        if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(child -> maskNode(child, rules));
        }
    }

    private boolean matchesField(MaskingRule rule, String fieldName) {
        String normalizedField = normalize(fieldName);
        return normalize(rule.dataType()).equals(normalizedField);
    }

    private List<SecurityPolicy> activeMaskingPolicies() {
        UUID currentTenantId = TenantContextHolder.currentTenantId().orElse(null);
        return securityPolicyRepository.findAll().stream()
                .filter(policy -> policy.policyType() == SecurityPolicyType.MASKING)
                .filter(policy -> policy.status() == SecurityPolicyStatus.ACTIVE)
                .filter(policy -> policy.tenantId() == null || policy.tenantId().equals(currentTenantId))
                .toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("_", "").replace("-", "").toLowerCase();
    }
}
