package com.hjo2oa.msg.ecosystem.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EcosystemIntegration(
        UUID id,
        IntegrationType integrationType,
        String displayName,
        AuthMode authMode,
        String callbackUrl,
        String signAlgorithm,
        String configRef,
        IntegrationStatus status,
        HealthStatus healthStatus,
        Instant lastCheckAt,
        String lastErrorSummary,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {

    public EcosystemIntegration {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(integrationType, "integrationType must not be null");
        displayName = requireText(displayName, "displayName");
        callbackUrl = normalizeOptional(callbackUrl);
        signAlgorithm = normalizeOptional(signAlgorithm);
        configRef = requireText(configRef, "configRef");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(healthStatus, "healthStatus must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static EcosystemIntegration create(
            UUID id,
            IntegrationType integrationType,
            String displayName,
            AuthMode authMode,
            String callbackUrl,
            String signAlgorithm,
            String configRef,
            UUID tenantId,
            Instant now
    ) {
        return new EcosystemIntegration(
                id,
                integrationType,
                displayName,
                authMode,
                callbackUrl,
                signAlgorithm,
                configRef,
                IntegrationStatus.DRAFT,
                HealthStatus.UNKNOWN,
                null,
                null,
                tenantId,
                now,
                now
        );
    }

    public EcosystemIntegration updateConfiguration(
            String nextDisplayName,
            AuthMode nextAuthMode,
            String nextCallbackUrl,
            String nextSignAlgorithm,
            String nextConfigRef,
            Instant now
    ) {
        return new EcosystemIntegration(
                id,
                integrationType,
                nextDisplayName,
                nextAuthMode,
                nextCallbackUrl,
                nextSignAlgorithm,
                nextConfigRef,
                status,
                healthStatus,
                lastCheckAt,
                lastErrorSummary,
                tenantId,
                createdAt,
                now
        );
    }

    public EcosystemIntegration changeStatus(IntegrationStatus nextStatus, Instant now) {
        return new EcosystemIntegration(
                id,
                integrationType,
                displayName,
                authMode,
                callbackUrl,
                signAlgorithm,
                configRef,
                Objects.requireNonNull(nextStatus, "status must not be null"),
                healthStatus,
                lastCheckAt,
                lastErrorSummary,
                tenantId,
                createdAt,
                now
        );
    }

    public EcosystemIntegration updateHealth(HealthStatus nextHealthStatus, String errorSummary, Instant now) {
        return new EcosystemIntegration(
                id,
                integrationType,
                displayName,
                authMode,
                callbackUrl,
                signAlgorithm,
                configRef,
                status,
                Objects.requireNonNull(nextHealthStatus, "healthStatus must not be null"),
                now,
                normalizeOptional(errorSummary),
                tenantId,
                createdAt,
                now
        );
    }

    public boolean available() {
        return status == IntegrationStatus.ENABLED && healthStatus == HealthStatus.HEALTHY;
    }

    public EcosystemIntegrationView toView() {
        return new EcosystemIntegrationView(
                id,
                integrationType,
                displayName,
                authMode,
                callbackUrl,
                signAlgorithm,
                configRef,
                status,
                healthStatus,
                lastCheckAt,
                lastErrorSummary,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    public IntegrationAvailabilityView toAvailabilityView() {
        return new IntegrationAvailabilityView(id, integrationType, configRef, healthStatus, available());
    }

    static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
