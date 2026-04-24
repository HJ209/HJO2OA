package com.hjo2oa.infra.cache.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CachePolicy(
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

    public CachePolicy {
        Objects.requireNonNull(id, "id must not be null");
        namespace = requireText(namespace, "namespace");
        Objects.requireNonNull(backendType, "backendType must not be null");
        requirePositive(ttlSeconds, "ttlSeconds");
        requirePositiveIfPresent(maxCapacity, "maxCapacity");
        Objects.requireNonNull(evictionPolicy, "evictionPolicy must not be null");
        Objects.requireNonNull(invalidationMode, "invalidationMode must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static CachePolicy create(
            String namespace,
            CacheBackendType backendType,
            int ttlSeconds,
            Integer maxCapacity,
            EvictionPolicy evictionPolicy,
            InvalidationMode invalidationMode,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");
        return new CachePolicy(
                UUID.randomUUID(),
                namespace,
                backendType,
                ttlSeconds,
                maxCapacity,
                evictionPolicy,
                invalidationMode,
                true,
                true,
                now,
                now
        );
    }

    public CachePolicy update(
            int ttlSeconds,
            Integer maxCapacity,
            EvictionPolicy evictionPolicy,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");
        return new CachePolicy(
                id,
                namespace,
                backendType,
                ttlSeconds,
                maxCapacity,
                evictionPolicy,
                invalidationMode,
                metricsEnabled,
                active,
                createdAt,
                now
        );
    }

    public CachePolicy deactivate(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new CachePolicy(
                id,
                namespace,
                backendType,
                ttlSeconds,
                maxCapacity,
                evictionPolicy,
                invalidationMode,
                metricsEnabled,
                false,
                createdAt,
                now
        );
    }

    public CachePolicyView toView() {
        return new CachePolicyView(
                id,
                namespace,
                backendType,
                ttlSeconds,
                maxCapacity,
                evictionPolicy,
                invalidationMode,
                metricsEnabled,
                active,
                createdAt,
                updatedAt
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static void requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
    }

    private static void requirePositiveIfPresent(Integer value, String fieldName) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
    }
}
