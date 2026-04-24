package com.hjo2oa.infra.security.application;

import com.hjo2oa.infra.security.domain.RateLimitSubjectType;
import com.hjo2oa.infra.security.domain.SecurityAnomalyDetectedEvent;
import com.hjo2oa.infra.security.domain.SecurityPolicy;
import com.hjo2oa.infra.security.domain.SecurityPolicyRepository;
import com.hjo2oa.infra.security.domain.SecurityPolicyStatus;
import com.hjo2oa.infra.security.domain.SecurityPolicyType;
import com.hjo2oa.infra.security.domain.SecurityPolicyUpdatedEvent;
import com.hjo2oa.infra.security.domain.SecurityPolicyView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SecurityPolicyApplicationService {

    private final SecurityPolicyRepository securityPolicyRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public SecurityPolicyApplicationService(
            SecurityPolicyRepository securityPolicyRepository,
            DomainEventPublisher domainEventPublisher
    ) {
        this(securityPolicyRepository, domainEventPublisher, Clock.systemUTC());
    }

    @Autowired
    public SecurityPolicyApplicationService(
            SecurityPolicyRepository securityPolicyRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.securityPolicyRepository = Objects.requireNonNull(
                securityPolicyRepository,
                "securityPolicyRepository must not be null"
        );
        this.domainEventPublisher = Objects.requireNonNull(
                domainEventPublisher,
                "domainEventPublisher must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public SecurityPolicyView createPolicy(
            String code,
            SecurityPolicyType type,
            String name,
            String configSnapshot,
            UUID tenantId
    ) {
        String normalizedCode = normalizeRequired(code, "code");
        if (securityPolicyRepository.findByPolicyCode(normalizedCode).isPresent()) {
            throw new BizException(
                    SecurityErrorDescriptors.SECURITY_POLICY_CODE_DUPLICATE,
                    "Security policy code already exists: " + normalizedCode
            );
        }
        Instant now = now();
        SecurityPolicy policy = SecurityPolicy.create(
                UUID.randomUUID(),
                normalizedCode,
                Objects.requireNonNull(type, "type must not be null"),
                name,
                configSnapshot,
                tenantId,
                now
        );
        return securityPolicyRepository.save(policy).toView();
    }

    public SecurityPolicyView disablePolicy(UUID policyId) {
        SecurityPolicy policy = requirePolicy(policyId);
        return securityPolicyRepository.save(policy.disable(now())).toView();
    }

    public SecurityPolicyView updateConfig(UUID policyId, String configSnapshot) {
        SecurityPolicy policy = requirePolicy(policyId);
        Instant now = now();
        SecurityPolicy updatedPolicy = securityPolicyRepository.save(policy.updateConfig(configSnapshot, now));
        domainEventPublisher.publish(SecurityPolicyUpdatedEvent.from(updatedPolicy, now));
        return updatedPolicy.toView();
    }

    public SecurityPolicyView addSecretKey(UUID policyId, String keyRef, String algorithm) {
        SecurityPolicy policy = requirePolicy(policyId);
        ensureKeyPolicy(policy);
        return securityPolicyRepository.save(policy.addSecretKey(keyRef, algorithm, now())).toView();
    }

    public SecurityPolicyView rotateKey(UUID policyId, UUID keyId) {
        SecurityPolicy policy = requirePolicy(policyId);
        ensureKeyPolicy(policy);
        try {
            return securityPolicyRepository.save(policy.rotateKey(keyId, now())).toView();
        } catch (IllegalArgumentException ex) {
            throw new BizException(SecurityErrorDescriptors.SECURITY_SECRET_KEY_NOT_FOUND, ex.getMessage(), ex);
        }
    }

    public SecurityPolicyView addMaskingRule(UUID policyId, String dataType, String ruleExpr) {
        SecurityPolicy policy = requirePolicy(policyId);
        ensurePolicyType(policy, SecurityPolicyType.MASKING, "masking rule");
        return securityPolicyRepository.save(policy.addMaskingRule(dataType, ruleExpr, now())).toView();
    }

    public SecurityPolicyView addRateLimitRule(
            UUID policyId,
            RateLimitSubjectType subjectType,
            int windowSeconds,
            int maxRequests
    ) {
        SecurityPolicy policy = requirePolicy(policyId);
        ensurePolicyType(policy, SecurityPolicyType.ACCESS_CONTROL, "rate limit rule");
        return securityPolicyRepository.save(policy.addRateLimitRule(subjectType, windowSeconds, maxRequests, now())).toView();
    }

    public void detectAnomaly(String policyCode, RateLimitSubjectType subjectType) {
        SecurityPolicy policy = requirePolicy(policyCode);
        domainEventPublisher.publish(SecurityAnomalyDetectedEvent.of(
                policy.policyCode(),
                subjectType,
                policy.tenantId() == null ? null : policy.tenantId().toString(),
                now()
        ));
    }

    public String maskValue(String policyCode, String dataType, String value) {
        SecurityPolicy policy = requirePolicy(policyCode);
        ensurePolicyType(policy, SecurityPolicyType.MASKING, "masking");
        if (policy.status() != SecurityPolicyStatus.ACTIVE) {
            throw new BizException(
                    SecurityErrorDescriptors.SECURITY_POLICY_RULE_VIOLATION,
                    "Security policy is not active: " + policy.policyCode()
            );
        }
        return policy.activeMaskingRule(dataType)
                .map(rule -> rule.mask(value))
                .orElse(value);
    }

    public List<SecurityPolicyView> listPolicies(
            SecurityPolicyType policyType,
            SecurityPolicyStatus status,
            UUID tenantId
    ) {
        return securityPolicyRepository.findAll().stream()
                .filter(policy -> policyType == null || policy.policyType() == policyType)
                .filter(policy -> status == null || policy.status() == status)
                .filter(policy -> tenantId == null || tenantId.equals(policy.tenantId()))
                .sorted((left, right) -> right.updatedAt().compareTo(left.updatedAt()))
                .map(SecurityPolicy::toView)
                .toList();
    }

    private SecurityPolicy requirePolicy(UUID policyId) {
        Objects.requireNonNull(policyId, "policyId must not be null");
        return securityPolicyRepository.findById(policyId)
                .orElseThrow(() -> new BizException(
                        SecurityErrorDescriptors.SECURITY_POLICY_NOT_FOUND,
                        "Security policy not found: " + policyId
                ));
    }

    private SecurityPolicy requirePolicy(String policyCode) {
        String normalizedCode = normalizeRequired(policyCode, "policyCode");
        return securityPolicyRepository.findByPolicyCode(normalizedCode)
                .orElseThrow(() -> new BizException(
                        SecurityErrorDescriptors.SECURITY_POLICY_NOT_FOUND,
                        "Security policy not found: " + normalizedCode
                ));
    }

    private void ensureKeyPolicy(SecurityPolicy policy) {
        if (policy.policyType() != SecurityPolicyType.KEY_MANAGEMENT
                && policy.policyType() != SecurityPolicyType.SIGNATURE) {
            throw new BizException(
                    SecurityErrorDescriptors.SECURITY_POLICY_RULE_VIOLATION,
                    "Secret keys are only supported by key management or signature policies"
            );
        }
    }

    private void ensurePolicyType(SecurityPolicy policy, SecurityPolicyType expectedType, String action) {
        if (policy.policyType() != expectedType) {
            throw new BizException(
                    SecurityErrorDescriptors.SECURITY_POLICY_RULE_VIOLATION,
                    "Security policy type does not support " + action + ": " + policy.policyType()
            );
        }
    }

    private Instant now() {
        return clock.instant();
    }

    private String normalizeRequired(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
