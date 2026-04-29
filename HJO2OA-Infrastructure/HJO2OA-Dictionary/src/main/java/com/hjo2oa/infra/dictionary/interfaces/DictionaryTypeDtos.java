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
            UUID tenantId
    ) {

        public DictionaryTypeCommands.CreateTypeCommand toCommand() {
            return new DictionaryTypeCommands.CreateTypeCommand(
                    code,
                    name,
                    category,
                    hierarchical,
                    cacheable,
                    tenantId
            );
        }
    }

    public record AddItemRequest(
            @NotBlank @Size(max = 64) String itemCode,
            @NotBlank @Size(max = 128) String displayName,
            UUID parentItemId,
            @PositiveOrZero Integer sortOrder
    ) {

        public DictionaryTypeCommands.AddItemCommand toCommand() {
            return new DictionaryTypeCommands.AddItemCommand(itemCode, displayName, parentItemId, sortOrder);
        }
    }

    public record UpdateItemRequest(
            @Size(max = 128) String displayName,
            @PositiveOrZero Integer sortOrder
    ) {

        public DictionaryTypeCommands.UpdateItemCommand toCommand() {
            return new DictionaryTypeCommands.UpdateItemCommand(displayName, sortOrder);
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
            String multiLangValue
    ) {
    }

    public record DictionaryTypeResponse(
            UUID id,
            String code,
            String name,
            String category,
            boolean hierarchical,
            boolean cacheable,
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
            List<SystemEnumItemResponse> items
    ) {
    }

    public record SystemEnumImportResponse(
            int discoveredTypes,
            int createdTypes,
            int createdItems,
            List<String> importedCodes
    ) {
    }
}
