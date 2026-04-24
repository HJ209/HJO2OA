package com.hjo2oa.infra.data.i18n.application;

import java.util.UUID;

public final class TranslationEntryCommands {

    private TranslationEntryCommands() {
    }

    public record CreateCommand(
            String entityType,
            String entityId,
            String fieldName,
            String locale,
            String value,
            UUID tenantId,
            UUID updatedBy
    ) {
    }

    public record UpdateCommand(
            UUID entryId,
            String value,
            UUID updatedBy
    ) {
    }

    public record ReviewCommand(
            UUID entryId,
            UUID updatedBy
    ) {
    }

    public record ResolveCommand(
            String entityType,
            String entityId,
            String fieldName,
            String locale,
            UUID tenantId,
            String fallbackLocale,
            String originalValue
    ) {
    }

    public record BatchSaveItemCommand(
            UUID entryId,
            String entityType,
            String entityId,
            String fieldName,
            String locale,
            String value,
            UUID tenantId,
            UUID updatedBy
    ) {
    }
}
