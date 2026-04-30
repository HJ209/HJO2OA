package com.hjo2oa.infra.cache.domain;

import java.time.Instant;

public record CacheRuntimeKeyView(
        String namespace,
        String tenantId,
        String key,
        CacheBackendType backendType,
        Instant expiresAt
) {
}
