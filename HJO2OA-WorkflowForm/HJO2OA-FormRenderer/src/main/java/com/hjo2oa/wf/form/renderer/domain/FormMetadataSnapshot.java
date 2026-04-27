package com.hjo2oa.wf.form.renderer.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FormMetadataSnapshot(
        UUID metadataId,
        String code,
        String name,
        Integer version,
        List<FieldDefinition> fields,
        Object layout,
        List<ValidationRule> validations,
        Map<String, Map<String, FieldPermission>> fieldPermissionMap,
        UUID tenantId
) {

    public FormMetadataSnapshot {
        fields = fields == null ? List.of() : List.copyOf(fields);
        validations = validations == null ? List.of() : List.copyOf(validations);
        fieldPermissionMap = fieldPermissionMap == null ? Map.of() : Map.copyOf(fieldPermissionMap);
    }
}
