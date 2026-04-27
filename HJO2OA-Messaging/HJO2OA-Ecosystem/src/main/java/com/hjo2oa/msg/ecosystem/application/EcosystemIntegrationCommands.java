package com.hjo2oa.msg.ecosystem.application;

import com.hjo2oa.msg.ecosystem.domain.AuthMode;
import com.hjo2oa.msg.ecosystem.domain.HealthStatus;
import com.hjo2oa.msg.ecosystem.domain.IntegrationStatus;
import com.hjo2oa.msg.ecosystem.domain.IntegrationType;
import java.util.UUID;

public final class EcosystemIntegrationCommands {

    private EcosystemIntegrationCommands() {
    }

    public record CreateIntegrationCommand(
            IntegrationType integrationType,
            String displayName,
            AuthMode authMode,
            String callbackUrl,
            String signAlgorithm,
            String configRef,
            UUID tenantId
    ) {
    }

    public record UpdateIntegrationCommand(
            UUID integrationId,
            String displayName,
            AuthMode authMode,
            String callbackUrl,
            String signAlgorithm,
            String configRef
    ) {
    }

    public record ChangeIntegrationStatusCommand(UUID integrationId, IntegrationStatus status) {
    }

    public record UpdateHealthCommand(UUID integrationId, HealthStatus healthStatus, String errorSummary) {
    }

    public record VerifyCallbackCommand(
            UUID tenantId,
            UUID integrationId,
            String callbackType,
            String idempotencyKey,
            String signature,
            String payload
    ) {
    }
}
