package com.hjo2oa.infra.dictionary.domain;

import java.util.UUID;

public record DictionaryItemView(
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
}
