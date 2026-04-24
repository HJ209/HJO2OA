package com.hjo2oa.data.common.support;

import java.util.Collection;
import java.util.Objects;

public final class Require {

    private Require() {
    }

    public static String text(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    public static <T> T nonNull(T value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " must not be null");
    }

    public static <T extends Collection<?>> T nonEmpty(T value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return value;
    }
}
