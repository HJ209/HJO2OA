package com.hjo2oa.wf.form.metadata.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record FormMetadata(
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

    private static final Comparator<FormFieldDefinition> FIELD_ORDER =
            Comparator.comparing(FormFieldDefinition::fieldCode);

    public FormMetadata {
        Objects.requireNonNull(id, "id must not be null");
        code = FormFieldDefinition.requireText(code, "code");
        name = FormFieldDefinition.requireText(name, "name");
        nameI18nKey = FormFieldDefinition.normalizeNullable(nameI18nKey);
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        Objects.requireNonNull(status, "status must not be null");
        fieldSchema = immutableFields(fieldSchema);
        Objects.requireNonNull(layout, "layout must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (status == FormMetadataStatus.PUBLISHED && publishedAt == null) {
            throw new IllegalArgumentException("publishedAt is required for published metadata");
        }
    }

    public static FormMetadata create(
            UUID id,
            String code,
            String name,
            String nameI18nKey,
            List<FormFieldDefinition> fieldSchema,
            JsonNode layout,
            JsonNode validations,
            JsonNode fieldPermissionMap,
            UUID tenantId,
            Instant now
    ) {
        return new FormMetadata(
                id,
                code,
                name,
                nameI18nKey,
                1,
                FormMetadataStatus.DRAFT,
                fieldSchema,
                layout,
                validations,
                fieldPermissionMap,
                tenantId,
                null,
                now,
                now
        );
    }

    public FormMetadata updateDraft(
            String newName,
            String newNameI18nKey,
            List<FormFieldDefinition> newFieldSchema,
            JsonNode newLayout,
            JsonNode newValidations,
            JsonNode newFieldPermissionMap,
            Instant now
    ) {
        ensureDraft();
        return new FormMetadata(
                id,
                code,
                newName,
                newNameI18nKey,
                version,
                status,
                newFieldSchema,
                newLayout,
                newValidations,
                newFieldPermissionMap,
                tenantId,
                publishedAt,
                createdAt,
                now
        );
    }

    public FormMetadata publish(Instant now) {
        ensureDraft();
        return new FormMetadata(
                id,
                code,
                name,
                nameI18nKey,
                version,
                FormMetadataStatus.PUBLISHED,
                fieldSchema,
                layout,
                validations,
                fieldPermissionMap,
                tenantId,
                now,
                createdAt,
                now
        );
    }

    public FormMetadata deprecate(Instant now) {
        if (status == FormMetadataStatus.DEPRECATED) {
            return this;
        }
        if (status != FormMetadataStatus.PUBLISHED) {
            throw new IllegalStateException("only published metadata can be deprecated");
        }
        return new FormMetadata(
                id,
                code,
                name,
                nameI18nKey,
                version,
                FormMetadataStatus.DEPRECATED,
                fieldSchema,
                layout,
                validations,
                fieldPermissionMap,
                tenantId,
                publishedAt,
                createdAt,
                now
        );
    }

    public FormMetadata deriveNewVersion(UUID newId, int newVersion, Instant now) {
        if (status == FormMetadataStatus.DRAFT) {
            throw new IllegalStateException("draft metadata cannot derive new version");
        }
        return new FormMetadata(
                newId,
                code,
                name,
                nameI18nKey,
                newVersion,
                FormMetadataStatus.DRAFT,
                fieldSchema,
                layout,
                validations,
                fieldPermissionMap,
                tenantId,
                null,
                now,
                now
        );
    }

    public FormMetadataView toView() {
        return new FormMetadataView(
                id,
                code,
                name,
                nameI18nKey,
                version,
                status,
                tenantId,
                publishedAt,
                createdAt,
                updatedAt
        );
    }

    public FormMetadataDetailView toDetailView() {
        return new FormMetadataDetailView(
                id,
                code,
                name,
                nameI18nKey,
                version,
                status,
                fieldSchema,
                layout,
                validations,
                fieldPermissionMap,
                tenantId,
                publishedAt,
                createdAt,
                updatedAt
        );
    }

    public FormRenderSchemaView toRenderSchemaView() {
        if (status != FormMetadataStatus.PUBLISHED) {
            throw new IllegalStateException("render schema requires published metadata");
        }
        return new FormRenderSchemaView(
                id,
                code,
                name,
                nameI18nKey,
                version,
                fieldSchema,
                layout,
                validations,
                fieldPermissionMap,
                tenantId,
                publishedAt
        );
    }

    public List<FormFieldDefinition> sortedFieldSchema() {
        return fieldSchema.stream().sorted(FIELD_ORDER).toList();
    }

    private void ensureDraft() {
        if (status != FormMetadataStatus.DRAFT) {
            throw new IllegalStateException("only draft metadata can be modified");
        }
    }

    private static List<FormFieldDefinition> immutableFields(List<FormFieldDefinition> fields) {
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("fieldSchema must not be empty");
        }
        Set<String> codes = new LinkedHashSet<>();
        List<FormFieldDefinition> normalized = new ArrayList<>();
        for (FormFieldDefinition field : fields) {
            if (field == null) {
                continue;
            }
            for (FormFieldDefinition flattened : field.flatten()) {
                if (!codes.add(flattened.fieldCode())) {
                    throw new IllegalArgumentException("duplicate fieldCode: " + flattened.fieldCode());
                }
            }
            normalized.add(field);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("fieldSchema must not be empty");
        }
        return List.copyOf(normalized);
    }
}
