package com.hjo2oa.infra.cache.application;

import com.hjo2oa.infra.cache.domain.CacheBackendType;
import com.hjo2oa.infra.cache.domain.EvictionPolicy;
import com.hjo2oa.infra.cache.domain.InvalidationMode;
import com.hjo2oa.infra.cache.domain.InvalidationReasonType;
import java.util.UUID;

public final class CachePolicyCommands {

    private CachePolicyCommands() {
    }

    public record CreatePolicyCommand(
            String namespace,
            CacheBackendType backendType,
            int ttlSeconds,
            Integer maxCapacity,
            EvictionPolicy evictionPolicy,
            InvalidationMode invalidationMode
    ) {
    }

    public record UpdatePolicyCommand(
            UUID policyId,
            Integer ttlSeconds,
            Integer maxCapacity,
            EvictionPolicy evictionPolicy
    ) {
    }

    public record InvalidateKeyCommand(
            String namespace,
            String key,
            InvalidationReasonType reasonType,
            String reasonRef
    ) {
    }
}
