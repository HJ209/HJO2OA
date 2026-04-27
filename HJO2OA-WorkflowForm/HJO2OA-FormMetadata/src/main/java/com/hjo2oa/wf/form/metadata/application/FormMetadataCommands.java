package com.hjo2oa.wf.form.metadata.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.hjo2oa.wf.form.metadata.domain.FormFieldDefinition;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataStatus;
import java.util.List;
import java.util.UUID;

public final class FormMetadataCommands {

    private FormMetadataCommands() {
    }

    public record SaveFormMetadataCommand(
            String code,
            String name,
            String nameI18nKey,
            List<FormFieldDefinition> fieldSchema,
            JsonNode layout,
            JsonNode validations,
            JsonNode fieldPermissionMap,
            UUID tenantId
    ) {
    }

    public record UpdateFormMetadataCommand(
            UUID metadataId,
            String name,
            String nameI18nKey,
            List<FormFieldDefinition> fieldSchema,
            JsonNode layout,
            JsonNode validations,
            JsonNode fieldPermissionMap
    ) {
    }

    public record FormMetadataQuery(
            UUID tenantId,
            String code,
            FormMetadataStatus status
    ) {
    }
}
