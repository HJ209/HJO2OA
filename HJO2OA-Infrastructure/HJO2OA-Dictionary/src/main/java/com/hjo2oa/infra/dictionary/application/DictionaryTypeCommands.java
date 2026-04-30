package com.hjo2oa.infra.dictionary.application;

import java.util.UUID;

public final class DictionaryTypeCommands {

    private DictionaryTypeCommands() {
    }

    public record CreateTypeCommand(
            String code,
            String name,
            String category,
            boolean hierarchical,
            boolean cacheable,
            Integer sortOrder,
            UUID tenantId
    ) {
    }

    public record UpdateTypeCommand(
            String name,
            String category,
            Boolean hierarchical,
            Boolean cacheable,
            Integer sortOrder
    ) {
    }

    public record AddItemCommand(
            String itemCode,
            String displayName,
            UUID parentItemId,
            Integer sortOrder,
            Boolean defaultItem,
            String multiLangValue,
            String extensionJson
    ) {
    }

    public record UpdateItemCommand(
            String displayName,
            UUID parentItemId,
            Integer sortOrder,
            Boolean defaultItem,
            String multiLangValue,
            String extensionJson
    ) {
    }

    public record ReorderItemCommand(
            UUID itemId,
            Integer sortOrder,
            UUID parentItemId
    ) {
    }
}
