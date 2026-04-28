package com.hjo2oa.infra.cache.infrastructure;

import com.hjo2oa.infra.cache.domain.CacheInvalidationRecord;
import com.hjo2oa.infra.cache.domain.CachePolicy;
import com.hjo2oa.infra.cache.domain.CachePolicyRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "hjo2oa.cache.type", havingValue = "inmemory", matchIfMissing = true)
public class InMemoryCachePolicyRepository implements CachePolicyRepository {

    private final Map<UUID, CachePolicy> policiesById = new ConcurrentHashMap<>();
    private final Map<UUID, CacheInvalidationRecord> invalidationRecordsById = new ConcurrentHashMap<>();

    @Override
    public Optional<CachePolicy> findById(UUID policyId) {
        return Optional.ofNullable(policiesById.get(policyId));
    }

    @Override
    public Optional<CachePolicy> findByNamespace(String namespace) {
        return policiesById.values().stream()
                .filter(policy -> policy.namespace().equals(namespace))
                .findFirst();
    }

    @Override
    public List<CachePolicy> findAll() {
        return policiesById.values().stream()
                .sorted(Comparator.comparing(CachePolicy::namespace).thenComparing(CachePolicy::id))
                .toList();
    }

    @Override
    public CachePolicy save(CachePolicy cachePolicy) {
        policiesById.put(cachePolicy.id(), cachePolicy);
        return cachePolicy;
    }

    @Override
    public CacheInvalidationRecord saveInvalidationRecord(CacheInvalidationRecord invalidationRecord) {
        invalidationRecordsById.put(invalidationRecord.id(), invalidationRecord);
        return invalidationRecord;
    }

    public List<CacheInvalidationRecord> invalidationRecords() {
        List<CacheInvalidationRecord> records = new ArrayList<>(invalidationRecordsById.values());
        records.sort(Comparator.comparing(CacheInvalidationRecord::invalidatedAt).reversed());
        return List.copyOf(records);
    }
}
