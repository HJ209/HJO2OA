package com.hjo2oa.portal.personalization.domain;

import java.util.Objects;

public record QuickAccessEntry(
        QuickAccessEntryType entryType,
        String targetCode,
        String targetLink,
        String icon,
        int sortOrder,
        boolean pinned
) {

    public QuickAccessEntry {
        Objects.requireNonNull(entryType, "entryType must not be null");
        targetCode = requireText(targetCode, "targetCode");
        targetLink = normalizeOptional(targetLink);
        icon = normalizeOptional(icon);
        if (entryType == QuickAccessEntryType.LINK && targetLink == null) {
            throw new IllegalArgumentException("targetLink must not be blank when entryType is LINK");
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
