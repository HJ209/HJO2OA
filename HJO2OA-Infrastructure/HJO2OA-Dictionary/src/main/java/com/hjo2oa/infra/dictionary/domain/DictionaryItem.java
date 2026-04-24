package com.hjo2oa.infra.dictionary.domain;

import java.util.Objects;
import java.util.UUID;

public record DictionaryItem(
        UUID id,
        UUID dictionaryTypeId,
        String itemCode,
        String displayName,
        UUID parentItemId,
        int sortOrder,
        boolean enabled,
        String multiLangValue
) {

    public DictionaryItem {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(dictionaryTypeId, "dictionaryTypeId must not be null");
        itemCode = requireText(itemCode, "itemCode");
        displayName = requireText(displayName, "displayName");
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must not be negative");
        }
        multiLangValue = normalizeNullableText(multiLangValue);
    }

    public DictionaryItem update(String newDisplayName, Integer newSortOrder) {
        return new DictionaryItem(
                id,
                dictionaryTypeId,
                itemCode,
                newDisplayName == null ? displayName : newDisplayName,
                parentItemId,
                newSortOrder == null ? sortOrder : newSortOrder,
                enabled,
                multiLangValue
        );
    }

    public DictionaryItem enable() {
        if (enabled) {
            return this;
        }
        return new DictionaryItem(
                id,
                dictionaryTypeId,
                itemCode,
                displayName,
                parentItemId,
                sortOrder,
                true,
                multiLangValue
        );
    }

    public DictionaryItem disable() {
        if (!enabled) {
            return this;
        }
        return new DictionaryItem(
                id,
                dictionaryTypeId,
                itemCode,
                displayName,
                parentItemId,
                sortOrder,
                false,
                multiLangValue
        );
    }

    public DictionaryItemView toView() {
        return new DictionaryItemView(
                id,
                dictionaryTypeId,
                itemCode,
                displayName,
                parentItemId,
                sortOrder,
                enabled,
                multiLangValue
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
