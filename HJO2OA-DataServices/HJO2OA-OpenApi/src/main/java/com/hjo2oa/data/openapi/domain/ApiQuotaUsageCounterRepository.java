package com.hjo2oa.data.openapi.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ApiQuotaUsageCounterRepository {

    Optional<ApiQuotaUsageCounter> findByWindow(String tenantId, String policyId, String clientCode, Instant windowStartedAt);

    List<ApiQuotaUsageCounter> findAllByTenant(String tenantId);

    ApiQuotaUsageCounter save(ApiQuotaUsageCounter counter);
}
