package com.hjo2oa.data.openapi.infrastructure;

import com.hjo2oa.data.openapi.domain.ApiQuotaUsageCounter;
import com.hjo2oa.data.openapi.domain.ApiQuotaUsageCounterRepository;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryApiQuotaUsageCounterRepository implements ApiQuotaUsageCounterRepository {

    private final Map<String, ApiQuotaUsageCounter> counters = new ConcurrentHashMap<>();

    @Override
    public Optional<ApiQuotaUsageCounter> findByWindow(
            String tenantId,
            String policyId,
            String clientCode,
            Instant windowStartedAt
    ) {
        return counters.values().stream()
                .filter(counter -> counter.tenantId().equals(tenantId))
                .filter(counter -> counter.policyId().equals(policyId))
                .filter(counter -> counter.clientCode().equals(clientCode))
                .filter(counter -> counter.windowStartedAt().equals(windowStartedAt))
                .findFirst();
    }

    @Override
    public List<ApiQuotaUsageCounter> findAllByTenant(String tenantId) {
        return counters.values().stream()
                .filter(counter -> counter.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public ApiQuotaUsageCounter save(ApiQuotaUsageCounter counter) {
        counters.put(counter.counterId(), counter);
        return counter;
    }
}
