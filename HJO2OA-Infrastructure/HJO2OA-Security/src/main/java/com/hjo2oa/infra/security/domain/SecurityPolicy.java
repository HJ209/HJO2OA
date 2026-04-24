package com.hjo2oa.infra.security.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record SecurityPolicy(
        UUID id,
        String policyCode,
        SecurityPolicyType policyType,
        String name,
        SecurityPolicyStatus status,
        UUID tenantId,
        String configSnapshot,
        Instant createdAt,
        Instant updatedAt,
        List<SecretKeyMaterial> secretKeys,
        List<MaskingRule> maskingRules,
        List<RateLimitRule> rateLimitRules
) {

    public SecurityPolicy {
        Objects.requireNonNull(id, "id must not be null");
        policyCode = requireText(policyCode, "policyCode");
        Objects.requireNonNull(policyType, "policyType must not be null");
        name = requireText(name, "name");
        Objects.requireNonNull(status, "status must not be null");
        configSnapshot = requireText(configSnapshot, "configSnapshot");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        secretKeys = List.copyOf(Objects.requireNonNull(secretKeys, "secretKeys must not be null"));
        maskingRules = List.copyOf(Objects.requireNonNull(maskingRules, "maskingRules must not be null"));
        rateLimitRules = List.copyOf(Objects.requireNonNull(rateLimitRules, "rateLimitRules must not be null"));
    }

    public static SecurityPolicy create(
            UUID id,
            String policyCode,
            SecurityPolicyType policyType,
            String name,
            String configSnapshot,
            UUID tenantId,
            Instant now
    ) {
        return new SecurityPolicy(
                Objects.requireNonNull(id, "id must not be null"),
                policyCode,
                policyType,
                name,
                SecurityPolicyStatus.ACTIVE,
                tenantId,
                configSnapshot,
                Objects.requireNonNull(now, "now must not be null"),
                now,
                List.of(),
                List.of(),
                List.of()
        );
    }

    public SecurityPolicy disable(Instant now) {
        return new SecurityPolicy(
                id,
                policyCode,
                policyType,
                name,
                SecurityPolicyStatus.DISABLED,
                tenantId,
                configSnapshot,
                createdAt,
                Objects.requireNonNull(now, "now must not be null"),
                secretKeys,
                maskingRules,
                rateLimitRules
        );
    }

    public SecurityPolicy updateConfig(String configSnapshot, Instant now) {
        return new SecurityPolicy(
                id,
                policyCode,
                policyType,
                name,
                status,
                tenantId,
                configSnapshot,
                createdAt,
                Objects.requireNonNull(now, "now must not be null"),
                secretKeys,
                maskingRules,
                rateLimitRules
        );
    }

    public SecurityPolicy addSecretKey(String keyRef, String algorithm, Instant now) {
        String normalizedKeyRef = requireText(keyRef, "keyRef");
        if (secretKeys.stream().anyMatch(key -> key.keyRef().equalsIgnoreCase(normalizedKeyRef))) {
            throw new IllegalArgumentException("Secret key already exists: " + normalizedKeyRef);
        }
        List<SecretKeyMaterial> updatedSecretKeys = new ArrayList<>(secretKeys);
        updatedSecretKeys.add(SecretKeyMaterial.create(UUID.randomUUID(), id, normalizedKeyRef, algorithm));
        return new SecurityPolicy(
                id,
                policyCode,
                policyType,
                name,
                status,
                tenantId,
                configSnapshot,
                createdAt,
                Objects.requireNonNull(now, "now must not be null"),
                updatedSecretKeys,
                maskingRules,
                rateLimitRules
        );
    }

    public SecurityPolicy rotateKey(UUID keyId, Instant now) {
        Objects.requireNonNull(keyId, "keyId must not be null");
        Instant rotatedAt = Objects.requireNonNull(now, "now must not be null");
        if (secretKeys.stream().noneMatch(secretKey -> secretKey.id().equals(keyId))) {
            throw new IllegalArgumentException("Secret key not found: " + keyId);
        }
        List<SecretKeyMaterial> updatedSecretKeys = secretKeys.stream()
                .map(secretKey -> rotate(secretKey, keyId, rotatedAt))
                .toList();
        return new SecurityPolicy(
                id,
                policyCode,
                policyType,
                name,
                status,
                tenantId,
                configSnapshot,
                createdAt,
                rotatedAt,
                updatedSecretKeys,
                maskingRules,
                rateLimitRules
        );
    }

    public SecurityPolicy addMaskingRule(String dataType, String ruleExpr, Instant now) {
        String normalizedDataType = requireText(dataType, "dataType");
        if (maskingRules.stream().anyMatch(rule -> rule.matches(normalizedDataType))) {
            throw new IllegalArgumentException("Masking rule already exists for dataType: " + normalizedDataType);
        }
        List<MaskingRule> updatedMaskingRules = new ArrayList<>(maskingRules);
        updatedMaskingRules.add(MaskingRule.create(UUID.randomUUID(), id, normalizedDataType, ruleExpr));
        return new SecurityPolicy(
                id,
                policyCode,
                policyType,
                name,
                status,
                tenantId,
                configSnapshot,
                createdAt,
                Objects.requireNonNull(now, "now must not be null"),
                secretKeys,
                updatedMaskingRules,
                rateLimitRules
        );
    }

    public SecurityPolicy addRateLimitRule(
            RateLimitSubjectType subjectType,
            int windowSeconds,
            int maxRequests,
            Instant now
    ) {
        Objects.requireNonNull(subjectType, "subjectType must not be null");
        if (rateLimitRules.stream().anyMatch(rule -> rule.active() && rule.subjectType() == subjectType)) {
            throw new IllegalArgumentException("Rate limit rule already exists for subjectType: " + subjectType);
        }
        List<RateLimitRule> updatedRateLimitRules = new ArrayList<>(rateLimitRules);
        updatedRateLimitRules.add(RateLimitRule.create(UUID.randomUUID(), id, subjectType, windowSeconds, maxRequests));
        return new SecurityPolicy(
                id,
                policyCode,
                policyType,
                name,
                status,
                tenantId,
                configSnapshot,
                createdAt,
                Objects.requireNonNull(now, "now must not be null"),
                secretKeys,
                maskingRules,
                updatedRateLimitRules
        );
    }

    public Optional<MaskingRule> activeMaskingRule(String dataType) {
        String normalizedDataType = requireText(dataType, "dataType");
        return maskingRules.stream().filter(rule -> rule.matches(normalizedDataType)).findFirst();
    }

    public SecurityPolicyView toView() {
        return new SecurityPolicyView(
                id,
                policyCode,
                policyType,
                name,
                status,
                tenantId,
                configSnapshot,
                createdAt,
                updatedAt,
                secretKeys.stream().map(SecretKeyMaterial::toView).toList(),
                maskingRules.stream().map(MaskingRule::toView).toList(),
                rateLimitRules.stream().map(RateLimitRule::toView).toList()
        );
    }

    private SecretKeyMaterial rotate(SecretKeyMaterial secretKey, UUID targetKeyId, Instant rotatedAt) {
        if (secretKey.id().equals(targetKeyId)) {
            return secretKey.activate(rotatedAt);
        }
        if (secretKey.keyStatus() == KeyStatus.ACTIVE) {
            return secretKey.markRotating(rotatedAt);
        }
        if (secretKey.keyStatus() == KeyStatus.ROTATING) {
            return secretKey.revoke(rotatedAt);
        }
        return secretKey;
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
