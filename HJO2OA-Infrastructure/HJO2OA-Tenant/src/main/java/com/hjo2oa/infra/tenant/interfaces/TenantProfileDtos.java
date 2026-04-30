package com.hjo2oa.infra.tenant.interfaces;

import com.hjo2oa.infra.tenant.application.TenantProfileCommands;
import com.hjo2oa.infra.tenant.domain.IsolationMode;
import com.hjo2oa.infra.tenant.domain.QuotaType;
import com.hjo2oa.infra.tenant.domain.TenantStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TenantProfileDtos {

    private TenantProfileDtos() {
    }

    public record CreateTenantRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @NotNull IsolationMode isolationMode,
            @Size(max = 64) String packageCode,
            @Size(max = 16) String defaultLocale,
            @Size(max = 64) String defaultTimezone,
            UUID adminAccountId,
            UUID adminPersonId
    ) {

        public TenantProfileCommands.CreateTenantCommand toCommand() {
            return new TenantProfileCommands.CreateTenantCommand(
                    code,
                    name,
                    isolationMode,
                    packageCode,
                    defaultLocale,
                    defaultTimezone,
                    adminAccountId,
                    adminPersonId
            );
        }
    }

    public record UpdateTenantRequest(
            @Size(max = 128) String name,
            @Size(max = 64) String packageCode,
            @Size(max = 16) String defaultLocale,
            @Size(max = 64) String defaultTimezone,
            UUID adminAccountId,
            UUID adminPersonId
    ) {

        public TenantProfileCommands.UpdateTenantCommand toCommand(UUID tenantId) {
            return new TenantProfileCommands.UpdateTenantCommand(
                    tenantId,
                    name,
                    packageCode,
                    defaultLocale,
                    defaultTimezone,
                    adminAccountId,
                    adminPersonId
            );
        }
    }

    public record UpdateQuotaRequest(
            @NotNull @PositiveOrZero Long limitValue,
            @PositiveOrZero Long warningThreshold
    ) {

        public TenantProfileCommands.UpdateQuotaCommand toCommand(UUID tenantId, QuotaType quotaType) {
            return new TenantProfileCommands.UpdateQuotaCommand(
                    tenantId,
                    quotaType,
                    limitValue,
                    warningThreshold
            );
        }
    }

    public record ConsumeQuotaRequest(
            @NotNull @Positive Long delta
    ) {

        public TenantProfileCommands.ConsumeQuotaCommand toCommand(UUID tenantId, QuotaType quotaType) {
            return new TenantProfileCommands.ConsumeQuotaCommand(tenantId, quotaType, delta);
        }
    }

    public record TenantProfileResponse(
            UUID id,
            String tenantCode,
            String name,
            TenantStatus status,
            IsolationMode isolationMode,
            String packageCode,
            String defaultLocale,
            String defaultTimezone,
            UUID adminAccountId,
            UUID adminPersonId,
            boolean initialized,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record TenantProfileDetailResponse(
            UUID id,
            String tenantCode,
            String name,
            TenantStatus status,
            IsolationMode isolationMode,
            String packageCode,
            String defaultLocale,
            String defaultTimezone,
            UUID adminAccountId,
            UUID adminPersonId,
            boolean initialized,
            Instant createdAt,
            Instant updatedAt,
            List<TenantQuotaResponse> quotas
    ) {

        public TenantProfileDetailResponse {
            quotas = List.copyOf(quotas);
        }
    }

    public record TenantQuotaResponse(
            UUID id,
            UUID tenantProfileId,
            QuotaType quotaType,
            long limitValue,
            long usedValue,
            Long warningThreshold,
            boolean warning
    ) {
    }
}
