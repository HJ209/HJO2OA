package com.hjo2oa.infra.cache.infrastructure.redis;

import com.hjo2oa.infra.cache.domain.CacheInvalidationRecord;
import com.hjo2oa.infra.cache.domain.CachePolicy;
import com.hjo2oa.infra.cache.domain.CachePolicyRepository;
import com.hjo2oa.shared.cache.RedisCacheOperations;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
@ConditionalOnProperty(name = "hjo2oa.cache.type", havingValue = "redis")
public class RedisCachePolicyRepository implements CachePolicyRepository {

    private static final String KEY_PREFIX = "hjo2oa:cache:policy";
    private static final String POLICY_INDEX_KEY = KEY_PREFIX + ":index";
    private static final String INVALIDATION_PREFIX = KEY_PREFIX + ":invalidation";
    private static final String INVALIDATION_INDEX_KEY = INVALIDATION_PREFIX + ":index";

    private final RedisCacheOperations redisCacheOperations;
    private final Duration policyTtl;
    private final Duration invalidationTtl;

    public RedisCachePolicyRepository(
            RedisCacheOperations redisCacheOperations,
            @Value("${hjo2oa.cache.policy-ttl-seconds:86400}") long policyTtlSeconds,
            @Value("${hjo2oa.cache.invalidation-ttl-seconds:86400}") long invalidationTtlSeconds
    ) {
        this.redisCacheOperations = redisCacheOperations;
        this.policyTtl = Duration.ofSeconds(policyTtlSeconds);
        this.invalidationTtl = Duration.ofSeconds(invalidationTtlSeconds);
    }

    @Override
    public Optional<CachePolicy> findById(UUID policyId) {
        return redisCacheOperations.get(policyKey(policyId), CachePolicy.class);
    }

    @Override
    public Optional<CachePolicy> findByNamespace(String namespace) {
        return findAll().stream()
                .filter(policy -> policy.namespace().equals(namespace))
                .findFirst();
    }

    @Override
    public List<CachePolicy> findAll() {
        return policyIds().stream()
                .map(this::findById)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(CachePolicy::namespace).thenComparing(CachePolicy::id))
                .toList();
    }

    @Override
    public CachePolicy save(CachePolicy cachePolicy) {
        redisCacheOperations.set(policyKey(cachePolicy.id()), cachePolicy, policyTtl);
        List<UUID> policyIds = policyIds();
        Set<UUID> updatedIds = new LinkedHashSet<>(policyIds);
        updatedIds.add(cachePolicy.id());
        redisCacheOperations.set(POLICY_INDEX_KEY, new ArrayList<>(updatedIds), policyTtl);
        return cachePolicy;
    }

    @Override
    public CacheInvalidationRecord saveInvalidationRecord(CacheInvalidationRecord invalidationRecord) {
        redisCacheOperations.set(invalidationKey(invalidationRecord.id()), invalidationRecord, invalidationTtl);
        List<UUID> invalidationIds = invalidationIds();
        Set<UUID> updatedIds = new LinkedHashSet<>(invalidationIds);
        updatedIds.add(invalidationRecord.id());
        redisCacheOperations.set(INVALIDATION_INDEX_KEY, new ArrayList<>(updatedIds), invalidationTtl);
        return invalidationRecord;
    }

    private List<UUID> policyIds() {
        return ids(POLICY_INDEX_KEY);
    }

    private List<UUID> invalidationIds() {
        return ids(INVALIDATION_INDEX_KEY);
    }

    @SuppressWarnings("unchecked")
    private List<UUID> ids(String key) {
        return redisCacheOperations.get(key, List.class).orElseGet(List::of).stream()
                .map(RedisCachePolicyRepository::toUuid)
                .toList();
    }

    private static UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String text) {
            return UUID.fromString(text);
        }
        throw new IllegalStateException("Redis cache policy index contains non-UUID value: " + value);
    }

    private static String policyKey(UUID policyId) {
        return KEY_PREFIX + ":" + policyId;
    }

    private static String invalidationKey(UUID invalidationRecordId) {
        return INVALIDATION_PREFIX + ":" + invalidationRecordId;
    }
}
