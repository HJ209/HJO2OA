package com.hjo2oa.msg.ecosystem.domain;

import java.time.Instant;
import java.util.UUID;

public record EcosystemIntegrationView(
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
}
