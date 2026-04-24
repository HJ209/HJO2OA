package com.hjo2oa.infra.tenant.infrastructure;

import com.hjo2oa.infra.tenant.domain.QuotaType;
import com.hjo2oa.infra.tenant.domain.TenantQuota;
import com.hjo2oa.infra.tenant.domain.TenantQuotaRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryTenantQuotaRepository implements TenantQuotaRepository {

    private final Map<UUID, TenantQuota> quotasById = new ConcurrentHashMap<>();

    @Override
    public Optional<TenantQuota> findByTenantProfileIdAndQuotaType(UUID tenantProfileId, QuotaType quotaType) {
        return quotasById.values().stream()
                .filter(quota -> quota.tenantProfileId().equals(tenantProfileId))
                .filter(quota -> quota.quotaType() == quotaType)
                .findFirst();
    }

    @Override
    public List<TenantQuota> findAllByTenantProfileId(UUID tenantProfileId) {
        return quotasById.values().stream()
                .filter(quota -> quota.tenantProfileId().equals(tenantProfileId))
                .toList();
    }

    @Override
    public TenantQuota save(TenantQuota quota) {
        quotasById.put(quota.id(), quota);
        return quota;
    }
}
