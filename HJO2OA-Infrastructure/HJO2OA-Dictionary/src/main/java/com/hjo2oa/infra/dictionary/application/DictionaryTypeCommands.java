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
            UUID tenantId
    ) {
    }

    public record AddItemCommand(
            String itemCode,
            String displayName,
            UUID parentItemId,
            Integer sortOrder
    ) {
    }

    public record UpdateItemCommand(
            String displayName,
            Integer sortOrder
    ) {
    }
}
