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
        String multiLangValue,
        boolean defaultItem,
        String extensionJson
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
        extensionJson = normalizeNullableText(extensionJson);
    }

    public DictionaryItem(
            UUID id,
            UUID dictionaryTypeId,
            String itemCode,
            String displayName,
            UUID parentItemId,
            int sortOrder,
            boolean enabled,
            String multiLangValue
    ) {
        this(id, dictionaryTypeId, itemCode, displayName, parentItemId, sortOrder, enabled, multiLangValue, false, null);
    }

    public DictionaryItem update(String newDisplayName, Integer newSortOrder) {
        return update(newDisplayName, parentItemId, newSortOrder, null, null, null);
    }

    public DictionaryItem update(
            String newDisplayName,
            UUID newParentItemId,
            Integer newSortOrder,
            Boolean newDefaultItem,
            String newMultiLangValue,
            String newExtensionJson
    ) {
        return new DictionaryItem(
                id,
                dictionaryTypeId,
                itemCode,
                newDisplayName == null ? displayName : newDisplayName,
                newParentItemId,
                newSortOrder == null ? sortOrder : newSortOrder,
                enabled,
                newMultiLangValue == null ? multiLangValue : newMultiLangValue,
                newDefaultItem == null ? defaultItem : newDefaultItem,
                newExtensionJson == null ? extensionJson : newExtensionJson
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
                multiLangValue,
                defaultItem,
                extensionJson
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
                multiLangValue,
                defaultItem,
                extensionJson
        );
    }

    public DictionaryItem asNonDefault() {
        if (!defaultItem) {
            return this;
        }
        return new DictionaryItem(
                id,
                dictionaryTypeId,
                itemCode,
                displayName,
                parentItemId,
                sortOrder,
                enabled,
                multiLangValue,
                false,
                extensionJson
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
                multiLangValue,
                defaultItem,
                extensionJson
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
