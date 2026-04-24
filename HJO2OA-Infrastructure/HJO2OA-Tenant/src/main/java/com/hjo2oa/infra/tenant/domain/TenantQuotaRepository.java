package com.hjo2oa.infra.tenant.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantQuotaRepository {

    Optional<TenantQuota> findByTenantProfileIdAndQuotaType(UUID tenantProfileId, QuotaType quotaType);

    List<TenantQuota> findAllByTenantProfileId(UUID tenantProfileId);

    TenantQuota save(TenantQuota quota);
}
