package com.hjo2oa.infra.tenant.domain;

import java.util.UUID;

public record TenantQuotaView(
        UUID id,
        UUID tenantProfileId,
        QuotaType quotaType,
        long limitValue,
        long usedValue,
        Long warningThreshold,
        boolean warning
) {
}
