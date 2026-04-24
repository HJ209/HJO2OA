package com.hjo2oa.infra.data.i18n.interfaces;

import com.hjo2oa.infra.data.i18n.application.TranslationEntryCommands;
import com.hjo2oa.infra.data.i18n.domain.TranslationResolveSource;
import com.hjo2oa.infra.data.i18n.domain.TranslationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TranslationEntryDtos {

    private TranslationEntryDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 64) String entityType,
            @NotBlank @Size(max = 64) String entityId,
            @NotBlank @Size(max = 128) String fieldName,
            @NotBlank @Size(max = 16) String locale,
            @NotBlank String value,
            @NotNull UUID tenantId
    ) {

        public TranslationEntryCommands.CreateCommand toCommand() {
            return new TranslationEntryCommands.CreateCommand(
                    entityType,
                    entityId,
                    fieldName,
                    locale,
                    value,
                    tenantId,
                    null
            );
        }
    }

    public record UpdateRequest(@NotBlank String value) {

        public TranslationEntryCommands.UpdateCommand toCommand(UUID entryId) {
            return new TranslationEntryCommands.UpdateCommand(entryId, value, null);
        }
    }

    public record ResolveRequest(
            @NotBlank @Size(max = 64) String entityType,
            @NotBlank @Size(max = 64) String entityId,
            @NotBlank @Size(max = 128) String fieldName,
            @NotBlank @Size(max = 16) String locale,
            @NotNull UUID tenantId,
            @Size(max = 16) String fallbackLocale,
            String originalValue
    ) {

        public TranslationEntryCommands.ResolveCommand toCommand() {
            return new TranslationEntryCommands.ResolveCommand(
                    entityType,
                    entityId,
                    fieldName,
                    locale,
                    tenantId,
                    fallbackLocale,
                    originalValue
            );
        }
    }

    public record BatchSaveRequest(@NotEmpty List<@Valid BatchSaveItemRequest> entries) {

        public List<TranslationEntryCommands.BatchSaveItemCommand> toCommands() {
            return entries.stream().map(BatchSaveItemRequest::toCommand).toList();
        }
    }

    public record BatchSaveItemRequest(
            UUID entryId,
            @NotBlank @Size(max = 64) String entityType,
            @NotBlank @Size(max = 64) String entityId,
            @NotBlank @Size(max = 128) String fieldName,
            @NotBlank @Size(max = 16) String locale,
            @NotBlank String value,
            @NotNull UUID tenantId
    ) {

        public TranslationEntryCommands.BatchSaveItemCommand toCommand() {
            return new TranslationEntryCommands.BatchSaveItemCommand(
                    entryId,
                    entityType,
                    entityId,
                    fieldName,
                    locale,
                    value,
                    tenantId,
                    null
            );
        }
    }

    public record EntryResponse(
            UUID id,
            String entityType,
            String entityId,
            String fieldName,
            String locale,
            String translatedValue,
            TranslationStatus translationStatus,
            UUID tenantId,
            UUID updatedBy,
            Instant updatedAt
    ) {
    }

    public record ResolveResponse(
            UUID entryId,
            String entityType,
            String entityId,
            String fieldName,
            String requestedLocale,
            String resolvedLocale,
            String resolvedValue,
            TranslationStatus translationStatus,
            TranslationResolveSource resolveSource,
            boolean fallbackApplied,
            UUID tenantId
    ) {
    }
}
