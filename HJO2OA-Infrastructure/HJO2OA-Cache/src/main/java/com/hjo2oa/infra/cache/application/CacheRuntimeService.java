package com.hjo2oa.infra.cache.application;

import com.hjo2oa.infra.cache.domain.CacheRuntimeKeyView;
import com.hjo2oa.infra.cache.domain.CacheRuntimeMetricsView;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public interface CacheRuntimeService {

    <T> Optional<T> get(String namespace, UUID tenantId, String key, Class<T> type);

    <T> T getOrLoad(
            String namespace,
            UUID tenantId,
            String key,
            Duration ttl,
            Class<T> type,
            Supplier<T> loader
    );

    void put(String namespace, UUID tenantId, String key, Object value, Duration ttl);

    boolean evictKey(String namespace, UUID tenantId, String key);

    int evictByPrefix(String namespace, UUID tenantId, String keyPrefix);

    int evictNamespace(String namespace);

    List<CacheRuntimeKeyView> findKeys(String namespace, UUID tenantId, String keyword);

    List<CacheRuntimeMetricsView> metrics();

    CacheRuntimeMetricsView metrics(String namespace);
}
