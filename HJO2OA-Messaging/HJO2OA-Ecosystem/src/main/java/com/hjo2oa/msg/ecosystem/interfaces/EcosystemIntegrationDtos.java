package com.hjo2oa.msg.ecosystem.interfaces;

import com.hjo2oa.msg.ecosystem.application.EcosystemIntegrationCommands;
import com.hjo2oa.msg.ecosystem.domain.AuthMode;
import com.hjo2oa.msg.ecosystem.domain.HealthStatus;
import com.hjo2oa.msg.ecosystem.domain.IntegrationStatus;
import com.hjo2oa.msg.ecosystem.domain.IntegrationType;
import com.hjo2oa.msg.ecosystem.domain.VerifyResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class EcosystemIntegrationDtos {

    private EcosystemIntegrationDtos() {
    }

    public record CreateIntegrationRequest(
            @NotNull IntegrationType integrationType,
            @NotBlank @Size(max = 128) String displayName,
            AuthMode authMode,
            @Size(max = 512) String callbackUrl,
            @Size(max = 64) String signAlgorithm,
            @NotBlank @Size(max = 128) String configRef,
            @NotNull UUID tenantId
    ) {

        public EcosystemIntegrationCommands.CreateIntegrationCommand toCommand() {
            return new EcosystemIntegrationCommands.CreateIntegrationCommand(
                    integrationType,
                    displayName,
                    authMode,
                    callbackUrl,
                    signAlgorithm,
                    configRef,
                    tenantId
            );
        }
    }

    public record UpdateIntegrationRequest(
            @NotBlank @Size(max = 128) String displayName,
            AuthMode authMode,
            @Size(max = 512) String callbackUrl,
            @Size(max = 64) String signAlgorithm,
            @NotBlank @Size(max = 128) String configRef
    ) {

        public EcosystemIntegrationCommands.UpdateIntegrationCommand toCommand(UUID integrationId) {
            return new EcosystemIntegrationCommands.UpdateIntegrationCommand(
                    integrationId,
                    displayName,
                    authMode,
                    callbackUrl,
                    signAlgorithm,
                    configRef
            );
        }
    }

    public record ChangeStatusRequest(@NotNull IntegrationStatus status) {
    }

    public record UpdateHealthRequest(@NotNull HealthStatus healthStatus, @Size(max = 512) String errorSummary) {
    }

    public record IntegrationResponse(
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

    public record AvailabilityResponse(
            UUID id,
            IntegrationType integrationType,
            String configRef,
            HealthStatus healthStatus,
            boolean available
    ) {
    }

    public record CallbackAuditResponse(
            UUID id,
            UUID integrationId,
            String callbackType,
            VerifyResult verifyResult,
            String payloadSummary,
            String errorMessage,
            String idempotencyKey,
            String payloadDigest,
            Instant occurredAt
    ) {
    }
}
