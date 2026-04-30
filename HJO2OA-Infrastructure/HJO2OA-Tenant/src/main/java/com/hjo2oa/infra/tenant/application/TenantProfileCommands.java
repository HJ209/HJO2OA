package com.hjo2oa.infra.tenant.application;

import com.hjo2oa.infra.tenant.domain.IsolationMode;
import com.hjo2oa.infra.tenant.domain.QuotaType;
import java.util.UUID;

public final class TenantProfileCommands {

    private TenantProfileCommands() {
    }

    public record CreateTenantCommand(
            String code,
            String name,
            IsolationMode isolationMode,
            String packageCode,
            String defaultLocale,
            String defaultTimezone,
            UUID adminAccountId,
            UUID adminPersonId
    ) {
    }

    public record UpdateTenantCommand(
            UUID tenantId,
            String name,
            String packageCode,
            String defaultLocale,
            String defaultTimezone,
            UUID adminAccountId,
            UUID adminPersonId
    ) {
    }

    public record UpdateQuotaCommand(
            UUID tenantId,
            QuotaType quotaType,
            long limitValue,
            Long warningThreshold
    ) {
    }

    public record CheckQuotaCommand(
            UUID tenantId,
            QuotaType quotaType
    ) {
    }

    public record ConsumeQuotaCommand(
            UUID tenantId,
            QuotaType quotaType,
            long delta
    ) {
    }
}
