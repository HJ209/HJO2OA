package com.hjo2oa.infra.security.interfaces;

import com.hjo2oa.infra.security.domain.KeyStatus;
import com.hjo2oa.infra.security.domain.RateLimitSubjectType;
import com.hjo2oa.infra.security.domain.SecurityPolicyStatus;
import com.hjo2oa.infra.security.domain.SecurityPolicyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SecurityPolicyDtos {

    private SecurityPolicyDtos() {
    }

    public record CreatePolicyRequest(
            @NotBlank @Size(max = 64) String policyCode,
            @NotNull SecurityPolicyType policyType,
            @NotBlank @Size(max = 128) String name,
            @NotBlank String configSnapshot,
            UUID tenantId
    ) {
    }

    public record UpdateConfigRequest(
            @NotBlank String configSnapshot
    ) {
    }

    public record AddSecretKeyRequest(
            @NotBlank @Size(max = 128) String keyRef,
            @NotBlank @Size(max = 64) String algorithm
    ) {
    }

    public record AddMaskingRuleRequest(
            @NotBlank @Size(max = 64) String dataType,
            @NotBlank @Size(max = 256) String ruleExpr
    ) {
    }

    public record AddRateLimitRuleRequest(
            @NotNull RateLimitSubjectType subjectType,
            @Min(1) int windowSeconds,
            @Min(1) int maxRequests
    ) {
    }

    public record MaskValueRequest(
            @NotBlank @Size(max = 64) String policyCode,
            @NotBlank @Size(max = 64) String dataType,
            String value
    ) {
    }

    public record MaskValueResponse(
            String maskedValue
    ) {
    }

    public record MaskingPreviewRequest(
            @NotBlank @Size(max = 64) String dataType,
            String value,
            @Size(max = 64) String policyCode
    ) {
    }

    public record CryptoRequest(
            @NotBlank @Size(max = 128) String keyRef,
            @NotBlank @Size(max = 16) String algorithm,
            @NotBlank String value
    ) {
    }

    public record CryptoResponse(
            String keyRef,
            String algorithm,
            String value
    ) {
    }

    public record KeyRotationResponse(
            String keyRef,
            int keyVersion
    ) {
    }

    public record PasswordValidationRequest(
            @NotBlank String password,
            @Size(max = 128) String username,
            @Size(max = 64) String policyCode,
            List<String> passwordHistory
    ) {
    }

    public record PasswordValidationResponse(
            boolean accepted,
            List<String> violations
    ) {
    }

    public record SecurityPolicyResponse(
            UUID id,
            String policyCode,
            SecurityPolicyType policyType,
            String name,
            SecurityPolicyStatus status,
            UUID tenantId,
            String configSnapshot,
            Instant createdAt,
            Instant updatedAt,
            List<SecretKeyMaterialResponse> secretKeys,
            List<MaskingRuleResponse> maskingRules,
            List<RateLimitRuleResponse> rateLimitRules
    ) {
    }

    public record SecretKeyMaterialResponse(
            UUID id,
            UUID securityPolicyId,
            String keyRef,
            String algorithm,
            KeyStatus keyStatus,
            Instant rotatedAt
    ) {
    }

    public record MaskingRuleResponse(
            UUID id,
            UUID securityPolicyId,
            String dataType,
            String ruleExpr,
            boolean active
    ) {
    }

    public record RateLimitRuleResponse(
            UUID id,
            UUID securityPolicyId,
            RateLimitSubjectType subjectType,
            int windowSeconds,
            int maxRequests,
            boolean active
    ) {
    }
}
