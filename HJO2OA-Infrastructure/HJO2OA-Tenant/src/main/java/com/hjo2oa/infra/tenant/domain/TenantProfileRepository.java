package com.hjo2oa.infra.tenant.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantProfileRepository {

    Optional<TenantProfile> findByCode(UUID tenantId, String code);

    Optional<TenantProfile> findByTenantId(UUID tenantId);

    TenantProfile save(TenantProfile profile);

    List<TenantProfile> findAllActive();
}
