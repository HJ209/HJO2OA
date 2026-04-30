package com.hjo2oa.infra.cache.interfaces;

import com.hjo2oa.infra.cache.application.CachePolicyCommands;
import com.hjo2oa.infra.cache.domain.CacheBackendType;
import com.hjo2oa.infra.cache.domain.EvictionPolicy;
import com.hjo2oa.infra.cache.domain.InvalidationMode;
import com.hjo2oa.infra.cache.domain.InvalidationReasonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class CachePolicyDtos {

    private CachePolicyDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 128) String namespace,
            @NotNull CacheBackendType backendType,
            @Positive int ttlSeconds,
            @Positive Integer maxCapacity,
            @NotNull EvictionPolicy evictionPolicy,
            @NotNull InvalidationMode invalidationMode
    ) {

        public CachePolicyCommands.CreatePolicyCommand toCommand() {
            return new CachePolicyCommands.CreatePolicyCommand(
                    namespace,
                    backendType,
                    ttlSeconds,
                    maxCapacity,
                    evictionPolicy,
                    invalidationMode
            );
        }
    }

    public record UpdateRequest(
            @Positive Integer ttlSeconds,
            @Positive Integer maxCapacity,
            EvictionPolicy evictionPolicy
    ) {

        public CachePolicyCommands.UpdatePolicyCommand toCommand(UUID policyId) {
            return new CachePolicyCommands.UpdatePolicyCommand(policyId, ttlSeconds, maxCapacity, evictionPolicy);
        }
    }

    public record InvalidateRequest(
            @NotBlank @Size(max = 128) String namespace,
            @NotBlank @Size(max = 256) String key,
            @NotNull InvalidationReasonType reasonType,
            @Size(max = 128) String reasonRef
    ) {

        public CachePolicyCommands.InvalidateKeyCommand toCommand() {
            return new CachePolicyCommands.InvalidateKeyCommand(namespace, key, reasonType, reasonRef);
        }
    }

    public record ClearNamespaceRequest(
            @Size(max = 128) String reasonRef
    ) {
    }

    public record PolicyResponse(
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

    public record InvalidationResponse(
            UUID id,
            UUID cachePolicyId,
            String namespace,
            String invalidateKey,
            InvalidationReasonType reasonType,
            String reasonRef,
            Instant invalidatedAt
    ) {
    }

    public record RuntimeKeyResponse(
            String namespace,
            String tenantId,
            String key,
            CacheBackendType backendType,
            Instant expiresAt
    ) {
    }

    public record RuntimeMetricsResponse(
            String namespace,
            long localHitCount,
            long redisHitCount,
            long missCount,
            long putCount,
            long invalidationCount,
            int keyCount
    ) {
    }
}
