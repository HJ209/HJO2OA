package com.hjo2oa.wf.form.metadata.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.hjo2oa.wf.form.metadata.application.FormMetadataCommands;
import com.hjo2oa.wf.form.metadata.domain.FormFieldDefinition;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FormMetadataDtos {

    private FormMetadataDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @Size(max = 256) String nameI18nKey,
            @NotEmpty List<@Valid FormFieldDefinition> fieldSchema,
            @NotNull JsonNode layout,
            JsonNode validations,
            JsonNode fieldPermissionMap,
            @NotNull UUID tenantId
    ) {

        public FormMetadataCommands.SaveFormMetadataCommand toCommand() {
            return new FormMetadataCommands.SaveFormMetadataCommand(
                    code,
                    name,
                    nameI18nKey,
                    fieldSchema,
                    layout,
                    validations,
                    fieldPermissionMap,
                    tenantId
            );
        }
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 128) String name,
            @Size(max = 256) String nameI18nKey,
            @NotEmpty List<@Valid FormFieldDefinition> fieldSchema,
            @NotNull JsonNode layout,
            JsonNode validations,
            JsonNode fieldPermissionMap
    ) {

        public FormMetadataCommands.UpdateFormMetadataCommand toCommand(UUID metadataId) {
            return new FormMetadataCommands.UpdateFormMetadataCommand(
                    metadataId,
                    name,
                    nameI18nKey,
                    fieldSchema,
                    layout,
                    validations,
                    fieldPermissionMap
            );
        }
    }

    public record FormMetadataResponse(
            UUID id,
            String code,
            String name,
            String nameI18nKey,
            int version,
            FormMetadataStatus status,
            UUID tenantId,
            Instant publishedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record FormMetadataDetailResponse(
            UUID id,
            String code,
            String name,
            String nameI18nKey,
            int version,
            FormMetadataStatus status,
            List<FormFieldDefinition> fieldSchema,
            JsonNode layout,
            JsonNode validations,
            JsonNode fieldPermissionMap,
            UUID tenantId,
            Instant publishedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record RenderSchemaResponse(
            UUID metadataId,
            String code,
            String name,
            String nameI18nKey,
            int version,
            List<FormFieldDefinition> fieldSchema,
            JsonNode layout,
            JsonNode validations,
            JsonNode fieldPermissionMap,
            UUID tenantId,
            Instant publishedAt
    ) {
    }
}
