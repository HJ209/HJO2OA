package com.hjo2oa.infra.dictionary.interfaces;

import com.hjo2oa.infra.dictionary.application.DictionaryTypeCommands;
import com.hjo2oa.infra.dictionary.domain.DictionaryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DictionaryTypeDtos {

    private DictionaryTypeDtos() {
    }

    public record CreateTypeRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @Size(max = 64) String category,
            @NotNull Boolean hierarchical,
            @NotNull Boolean cacheable,
            @PositiveOrZero Integer sortOrder,
            UUID tenantId
    ) {

        public DictionaryTypeCommands.CreateTypeCommand toCommand() {
            return new DictionaryTypeCommands.CreateTypeCommand(
                    code,
                    name,
                    category,
                    hierarchical,
                    cacheable,
                    sortOrder,
                    tenantId
            );
        }
    }

    public record UpdateTypeRequest(
            @Size(max = 128) String name,
            @Size(max = 64) String category,
            Boolean hierarchical,
            Boolean cacheable,
            @PositiveOrZero Integer sortOrder
    ) {

        public DictionaryTypeCommands.UpdateTypeCommand toCommand() {
            return new DictionaryTypeCommands.UpdateTypeCommand(name, category, hierarchical, cacheable, sortOrder);
        }
    }

    public record AddItemRequest(
            @NotBlank @Size(max = 64) String itemCode,
            @NotBlank @Size(max = 128) String displayName,
            UUID parentItemId,
            @PositiveOrZero Integer sortOrder,
            Boolean defaultItem,
            String multiLangValue,
            String extensionJson
    ) {

        public DictionaryTypeCommands.AddItemCommand toCommand() {
            return new DictionaryTypeCommands.AddItemCommand(
                    itemCode,
                    displayName,
                    parentItemId,
                    sortOrder,
                    defaultItem,
                    multiLangValue,
                    extensionJson
            );
        }
    }

    public record UpdateItemRequest(
            @Size(max = 128) String displayName,
            UUID parentItemId,
            @PositiveOrZero Integer sortOrder,
            Boolean defaultItem,
            String multiLangValue,
            String extensionJson
    ) {

        public DictionaryTypeCommands.UpdateItemCommand toCommand() {
            return new DictionaryTypeCommands.UpdateItemCommand(
                    displayName,
                    parentItemId,
                    sortOrder,
                    defaultItem,
                    multiLangValue,
                    extensionJson
            );
        }
    }

    public record ReorderItemRequest(
            @NotNull UUID itemId,
            @PositiveOrZero Integer sortOrder,
            UUID parentItemId
    ) {

        public DictionaryTypeCommands.ReorderItemCommand toCommand() {
            return new DictionaryTypeCommands.ReorderItemCommand(itemId, sortOrder, parentItemId);
        }
    }

    public record DictionaryItemResponse(
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

    public record DictionaryTypeResponse(
            UUID id,
            String code,
            String name,
            String category,
            boolean hierarchical,
            boolean cacheable,
            int sortOrder,
            boolean systemManaged,
            DictionaryStatus status,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt,
            List<DictionaryItemResponse> items
    ) {
    }

    public record SystemEnumItemResponse(
            String code,
            String name,
            int sortOrder
    ) {
    }

    public record SystemEnumDictionaryResponse(
            String code,
            String name,
            String className,
            String category,
            boolean imported,
            List<String> newItemCodes,
            List<String> changedItemCodes,
            List<String> disabledItemCodes,
            List<SystemEnumItemResponse> items
    ) {
    }

    public record SystemEnumImportResponse(
            int discoveredTypes,
            int createdTypes,
            int createdItems,
            int updatedItems,
            int disabledItems,
            List<String> importedCodes
    ) {
    }

    public record RuntimeItemResponse(
            UUID id,
            String code,
            String label,
            String value,
            UUID parentId,
            int sortOrder,
            boolean enabled,
            boolean defaultItem,
            String extensionJson,
            List<RuntimeItemResponse> children
    ) {
    }

    public record RuntimeDictionaryResponse(
            UUID id,
            String code,
            String name,
            String category,
            boolean hierarchical,
            UUID tenantId,
            String language,
            List<RuntimeItemResponse> items
    ) {
    }

    public record BatchRuntimeRequest(
            @NotNull List<@NotBlank String> codes,
            Boolean enabledOnly,
            Boolean tree
    ) {
    }
}
