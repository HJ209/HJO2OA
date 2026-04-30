package com.hjo2oa.infra.tenant.domain;

import java.util.Objects;
import java.util.UUID;

public record TenantQuota(
        UUID id,
        UUID tenantProfileId,
        QuotaType quotaType,
        long limitValue,
        long usedValue,
        Long warningThreshold
) {

    public TenantQuota {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(tenantProfileId, "tenantProfileId must not be null");
        Objects.requireNonNull(quotaType, "quotaType must not be null");
        if (limitValue < 0) {
            throw new IllegalArgumentException("limitValue must not be negative");
        }
        if (usedValue < 0) {
            throw new IllegalArgumentException("usedValue must not be negative");
        }
        if (usedValue > limitValue) {
            throw new IllegalArgumentException("usedValue must not be greater than limitValue");
        }
        if (warningThreshold != null && warningThreshold < 0) {
            throw new IllegalArgumentException("warningThreshold must not be negative");
        }
        if (warningThreshold != null && warningThreshold > limitValue) {
            throw new IllegalArgumentException("warningThreshold must not be greater than limitValue");
        }
    }

    public TenantQuota incrementUsage(long delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("delta must be greater than 0");
        }
        if (usedValue + delta > limitValue) {
            throw new IllegalArgumentException("quota usage exceeds limitValue");
        }
        return new TenantQuota(
                id,
                tenantProfileId,
                quotaType,
                limitValue,
                usedValue + delta,
                warningThreshold
        );
    }

    public TenantQuota resetUsage() {
        if (usedValue == 0) {
            return this;
        }
        return new TenantQuota(id, tenantProfileId, quotaType, limitValue, 0, warningThreshold);
    }

    public boolean isWarning() {
        return warningThreshold != null && usedValue >= warningThreshold;
    }

    public TenantQuotaView toView() {
        return new TenantQuotaView(
                id,
                tenantProfileId,
                quotaType,
                limitValue,
                usedValue,
                warningThreshold,
                isWarning()
        );
    }
}
