package com.hjo2oa.data.governance.domain;

import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceProfileStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record GovernanceProfile(
        String governanceId,
        String code,
        GovernanceScopeType scopeType,
        String targetCode,
        String slaPolicyJson,
        String alertPolicyJson,
        GovernanceProfileStatus status,
        String tenantId,
        Instant createdAt,
        Instant updatedAt,
        List<HealthCheckRule> healthCheckRules,
        List<AlertRule> alertRules,
        List<ServiceVersionRecord> serviceVersionRecords
) {

    public GovernanceProfile {
        governanceId = requireText(governanceId, "governanceId");
        code = requireText(code, "code");
        Objects.requireNonNull(scopeType, "scopeType must not be null");
        targetCode = requireText(targetCode, "targetCode");
        Objects.requireNonNull(status, "status must not be null");
        tenantId = requireText(tenantId, "tenantId");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        healthCheckRules = List.copyOf(Objects.requireNonNull(healthCheckRules, "healthCheckRules must not be null"));
        alertRules = List.copyOf(Objects.requireNonNull(alertRules, "alertRules must not be null"));
        serviceVersionRecords = List.copyOf(
                Objects.requireNonNull(serviceVersionRecords, "serviceVersionRecords must not be null")
        );
    }

    public static GovernanceProfile create(
            String governanceId,
            String code,
            GovernanceScopeType scopeType,
            String targetCode,
            String slaPolicyJson,
            String alertPolicyJson,
            String tenantId,
            Instant now
    ) {
        return new GovernanceProfile(
                governanceId,
                code,
                scopeType,
                targetCode,
                normalizeOptional(slaPolicyJson),
                normalizeOptional(alertPolicyJson),
                GovernanceProfileStatus.ACTIVE,
                tenantId,
                now,
                now,
                List.of(),
                List.of(),
                List.of()
        );
    }

    public GovernanceProfile updatePolicies(
            String slaPolicyJson,
            String alertPolicyJson,
            GovernanceProfileStatus status,
            Instant now
    ) {
        return new GovernanceProfile(
                governanceId,
                code,
                scopeType,
                targetCode,
                normalizeOptional(slaPolicyJson),
                normalizeOptional(alertPolicyJson),
                Objects.requireNonNull(status, "status must not be null"),
                tenantId,
                createdAt,
                now,
                healthCheckRules,
                alertRules,
                serviceVersionRecords
        );
    }

    public GovernanceProfile replaceHealthCheckRules(List<HealthCheckRule> rules, Instant now) {
        return new GovernanceProfile(
                governanceId,
                code,
                scopeType,
                targetCode,
                slaPolicyJson,
                alertPolicyJson,
                status,
                tenantId,
                createdAt,
                now,
                rules,
                alertRules,
                serviceVersionRecords
        );
    }

    public GovernanceProfile replaceAlertRules(List<AlertRule> rules, Instant now) {
        return new GovernanceProfile(
                governanceId,
                code,
                scopeType,
                targetCode,
                slaPolicyJson,
                alertPolicyJson,
                status,
                tenantId,
                createdAt,
                now,
                healthCheckRules,
                rules,
                serviceVersionRecords
        );
    }

    public GovernanceProfile replaceServiceVersionRecords(List<ServiceVersionRecord> records, Instant now) {
        return new GovernanceProfile(
                governanceId,
                code,
                scopeType,
                targetCode,
                slaPolicyJson,
                alertPolicyJson,
                status,
                tenantId,
                createdAt,
                now,
                healthCheckRules,
                alertRules,
                records
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
