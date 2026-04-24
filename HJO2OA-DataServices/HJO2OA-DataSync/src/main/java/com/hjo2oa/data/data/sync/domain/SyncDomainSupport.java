package com.hjo2oa.data.data.sync.domain;

import java.util.Objects;
import java.util.UUID;

final class SyncDomainSupport {

    private SyncDomainSupport() {
    }

    static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    static UUID requireId(UUID value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " must not be null");
    }
}
