package com.hjo2oa.infra.cache.application;

import com.hjo2oa.infra.cache.domain.CacheRuntimeKeyView;
import com.hjo2oa.infra.cache.domain.CacheRuntimeMetricsView;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class NoopCacheRuntimeService implements CacheRuntimeService {

    @Override
    public <T> Optional<T> get(String namespace, UUID tenantId, String key, Class<T> type) {
        return Optional.empty();
    }

    @Override
    public <T> T getOrLoad(
            String namespace,
            UUID tenantId,
            String key,
            Duration ttl,
            Class<T> type,
            Supplier<T> loader
    ) {
        return loader.get();
    }

    @Override
    public void put(String namespace, UUID tenantId, String key, Object value, Duration ttl) {
    }

    @Override
    public boolean evictKey(String namespace, UUID tenantId, String key) {
        return false;
    }

    @Override
    public int evictByPrefix(String namespace, UUID tenantId, String keyPrefix) {
        return 0;
    }

    @Override
    public int evictNamespace(String namespace) {
        return 0;
    }

    @Override
    public List<CacheRuntimeKeyView> findKeys(String namespace, UUID tenantId, String keyword) {
        return List.of();
    }

    @Override
    public List<CacheRuntimeMetricsView> metrics() {
        return List.of();
    }

    @Override
    public CacheRuntimeMetricsView metrics(String namespace) {
        return new CacheRuntimeMetricsView(namespace, 0, 0, 0, 0, 0, 0);
    }
}
