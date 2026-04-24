package com.hjo2oa.infra.cache.domain;

import java.time.Instant;
import java.util.UUID;

public record CachePolicyView(
        UUID id,
        String namespace,
        CacheBackendType backendType,
        int ttlSeconds,
        Integer maxCapacity,
        EvictionPolicy evictionPolicy,
        InvalidationMode invalidationMode,
        boolean metricsEnabled,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
