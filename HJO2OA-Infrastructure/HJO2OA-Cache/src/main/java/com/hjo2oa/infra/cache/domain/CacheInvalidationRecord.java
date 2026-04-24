package com.hjo2oa.infra.cache.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CacheInvalidationRecord(
        UUID id,
        UUID cachePolicyId,
        String invalidateKey,
        InvalidationReasonType reasonType,
        String reasonRef,
        Instant invalidatedAt
) {

    public CacheInvalidationRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(cachePolicyId, "cachePolicyId must not be null");
        invalidateKey = requireText(invalidateKey, "invalidateKey");
        Objects.requireNonNull(reasonType, "reasonType must not be null");
        reasonRef = normalizeNullableText(reasonRef);
        Objects.requireNonNull(invalidatedAt, "invalidatedAt must not be null");
    }

    public static CacheInvalidationRecord create(
            UUID cachePolicyId,
            String invalidateKey,
            InvalidationReasonType reasonType,
            String reasonRef,
            Instant invalidatedAt
    ) {
        return new CacheInvalidationRecord(
                UUID.randomUUID(),
                cachePolicyId,
                invalidateKey,
                reasonType,
                reasonRef,
                invalidatedAt
        );
    }

    public CacheInvalidationView toView(String namespace) {
        return new CacheInvalidationView(
                id,
                cachePolicyId,
                requireText(namespace, "namespace"),
                invalidateKey,
                reasonType,
                reasonRef,
                invalidatedAt
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

    private static String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
