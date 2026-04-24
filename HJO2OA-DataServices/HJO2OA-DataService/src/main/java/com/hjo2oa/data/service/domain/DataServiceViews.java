package com.hjo2oa.data.service.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DataServiceViews {

    private DataServiceViews() {
    }

    public record SummaryView(
            UUID serviceId,
            UUID tenantId,
            String code,
            String name,
            DataServiceDefinition.ServiceType serviceType,
            DataServiceDefinition.SourceMode sourceMode,
            DataServiceDefinition.PermissionMode permissionMode,
            DataServiceDefinition.Status status,
            boolean cacheEnabled,
            Instant activatedAt,
            int openApiReferenceCount,
            int reportReferenceCount,
            int syncReferenceCount,
            boolean openApiReusable,
            boolean reportReusable,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record DetailView(
            UUID serviceId,
            UUID tenantId,
            String code,
            String name,
            DataServiceDefinition.ServiceType serviceType,
            DataServiceDefinition.SourceMode sourceMode,
            DataServiceDefinition.PermissionMode permissionMode,
            DataServiceDefinition.PermissionBoundary permissionBoundary,
            DataServiceDefinition.CachePolicy cachePolicy,
            DataServiceDefinition.Status status,
            String sourceRef,
            String connectorId,
            String description,
            int statusSequence,
            Instant activatedAt,
            String createdBy,
            String updatedBy,
            Instant createdAt,
            Instant updatedAt,
            boolean openApiReusable,
            boolean reportReusable,
            List<ParameterView> parameters,
            List<FieldMappingView> fieldMappings
    ) {

        public DetailView {
            parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters must not be null"));
            fieldMappings = List.copyOf(Objects.requireNonNull(fieldMappings, "fieldMappings must not be null"));
        }
    }

    public record ParameterView(
            UUID parameterId,
            String paramCode,
            ServiceParameterDefinition.ParameterType paramType,
            boolean required,
            String defaultValue,
            ServiceParameterDefinition.ValidationRule validationRule,
            boolean enabled,
            String description,
            int sortOrder
    ) {
    }

    public record FieldMappingView(
            UUID mappingId,
            String sourceField,
            String targetField,
            ServiceFieldMapping.TransformRule transformRule,
            boolean masked,
            String description,
            int sortOrder
    ) {
    }

    public record ReusableView(
            UUID serviceId,
            UUID tenantId,
            String code,
            String name,
            DataServiceDefinition.ServiceType serviceType,
            DataServiceDefinition.SourceMode sourceMode,
            DataServiceDefinition.PermissionMode permissionMode,
            DataServiceDefinition.CachePolicy cachePolicy,
            Instant activatedAt,
            boolean openApiReusable,
            boolean reportReusable
    ) {
    }

    public record ExecutionPlan(
            UUID serviceId,
            String serviceCode,
            DataServiceDefinition.ServiceType serviceType,
            DataServiceDefinition.SourceMode sourceMode,
            DataServiceDefinition.PermissionMode permissionMode,
            boolean cacheEnabled,
            String cacheKey,
            Long cacheTtlSeconds,
            String appCode,
            String subjectId,
            String idempotencyKey,
            Map<String, Object> normalizedParameters,
            List<FieldMappingView> outputMappings,
            List<String> outputFields,
            boolean openApiReusable,
            boolean reportReusable,
            Instant preparedAt
    ) {

        public ExecutionPlan {
            normalizedParameters = Map.copyOf(Objects.requireNonNull(
                    normalizedParameters,
                    "normalizedParameters must not be null"
            ));
            outputMappings = List.copyOf(Objects.requireNonNull(outputMappings, "outputMappings must not be null"));
            outputFields = List.copyOf(Objects.requireNonNull(outputFields, "outputFields must not be null"));
        }
    }
}
