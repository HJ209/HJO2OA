package com.hjo2oa.infra.cache.domain;

import java.time.Instant;
import java.util.UUID;

public record CacheInvalidationView(
        UUID id,
        UUID cachePolicyId,
        String namespace,
        String invalidateKey,
        InvalidationReasonType reasonType,
        String reasonRef,
        Instant invalidatedAt
) {
}
