package com.hjo2oa.infra.cache.domain;

public record CacheRuntimeMetricsView(
        String namespace,
        long localHitCount,
        long redisHitCount,
        long missCount,
        long putCount,
        long invalidationCount,
        int keyCount
) {
}
