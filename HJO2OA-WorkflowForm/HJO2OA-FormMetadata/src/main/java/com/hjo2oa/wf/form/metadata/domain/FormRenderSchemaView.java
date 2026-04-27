package com.hjo2oa.wf.form.metadata.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FormRenderSchemaView(
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
