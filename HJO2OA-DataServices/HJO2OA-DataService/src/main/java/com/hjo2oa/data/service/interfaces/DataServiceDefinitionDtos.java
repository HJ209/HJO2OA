package com.hjo2oa.data.service.interfaces;

import com.hjo2oa.data.service.application.DataServiceDefinitionCommands;
import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.domain.ServiceFieldMapping;
import com.hjo2oa.data.service.domain.ServiceParameterDefinition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DataServiceDefinitionDtos {

    private DataServiceDefinitionDtos() {
    }

    public record CreateRequest(
            @NotNull UUID serviceId,
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @NotNull DataServiceDefinition.ServiceType serviceType,
            @NotNull DataServiceDefinition.SourceMode sourceMode,
            @NotNull DataServiceDefinition.PermissionMode permissionMode,
            @Valid PermissionBoundaryPayload permissionBoundary,
            @Valid CachePolicyPayload cachePolicy,
            @NotBlank @Size(max = 128) String sourceRef,
            @Size(max = 64) String connectorId,
            @Size(max = 512) String description,
            @Valid List<ParameterPayload> parameters,
            @Valid List<FieldMappingPayload> fieldMappings
    ) {

        public DataServiceDefinitionCommands.CreateCommand toCommand() {
            return new DataServiceDefinitionCommands.CreateCommand(
                    serviceId,
                    code,
                    name,
                    serviceType,
                    sourceMode,
                    permissionMode,
                    permissionBoundary == null ? null : permissionBoundary.toDomain(),
                    cachePolicy == null ? null : cachePolicy.toDomain(),
                    sourceRef,
                    connectorId,
                    description,
                    parameters == null ? List.of() : parameters.stream().map(ParameterPayload::toCommand).toList(),
                    fieldMappings == null
                            ? List.of()
                            : fieldMappings.stream().map(FieldMappingPayload::toCommand).toList()
            );
        }
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @NotNull DataServiceDefinition.ServiceType serviceType,
            @NotNull DataServiceDefinition.SourceMode sourceMode,
            @NotNull DataServiceDefinition.PermissionMode permissionMode,
            @Valid PermissionBoundaryPayload permissionBoundary,
            @Valid CachePolicyPayload cachePolicy,
            @NotBlank @Size(max = 128) String sourceRef,
            @Size(max = 64) String connectorId,
            @Size(max = 512) String description,
            @Valid List<ParameterPayload> parameters,
            @Valid List<FieldMappingPayload> fieldMappings
    ) {

        public DataServiceDefinitionCommands.UpdateCommand toCommand(UUID serviceId) {
            return new DataServiceDefinitionCommands.UpdateCommand(
                    serviceId,
                    code,
                    name,
                    serviceType,
                    sourceMode,
                    permissionMode,
                    permissionBoundary == null ? null : permissionBoundary.toDomain(),
                    cachePolicy == null ? null : cachePolicy.toDomain(),
                    sourceRef,
                    connectorId,
                    description,
                    parameters == null ? null : parameters.stream().map(ParameterPayload::toCommand).toList(),
                    fieldMappings == null ? null : fieldMappings.stream().map(FieldMappingPayload::toCommand).toList()
            );
        }
    }

    public record UpsertParameterRequest(
            UUID parameterId,
            @NotNull ServiceParameterDefinition.ParameterType paramType,
            Boolean required,
            @Size(max = 512) String defaultValue,
            @Valid ValidationRulePayload validationRule,
            Boolean enabled,
            @Size(max = 512) String description,
            @PositiveOrZero Integer sortOrder
    ) {

        public DataServiceDefinitionCommands.ParameterCommand toCommand(String paramCode) {
            return new DataServiceDefinitionCommands.ParameterCommand(
                    parameterId,
                    paramCode,
                    paramType,
                    Boolean.TRUE.equals(required),
                    defaultValue,
                    validationRule == null ? null : validationRule.toDomain(),
                    enabled == null || enabled,
                    description,
                    sortOrder == null ? 0 : sortOrder
            );
        }
    }

    public record UpsertFieldMappingRequest(
            @NotBlank @Size(max = 128) String sourceField,
            @NotBlank @Size(max = 128) String targetField,
            @Valid TransformRulePayload transformRule,
            Boolean masked,
            @Size(max = 512) String description,
            @PositiveOrZero Integer sortOrder
    ) {

        public DataServiceDefinitionCommands.FieldMappingCommand toCommand(UUID mappingId) {
            return new DataServiceDefinitionCommands.FieldMappingCommand(
                    mappingId,
                    sourceField,
                    targetField,
                    transformRule == null ? null : transformRule.toDomain(),
                    Boolean.TRUE.equals(masked),
                    description,
                    sortOrder == null ? 0 : sortOrder
            );
        }
    }

    public record PermissionBoundaryPayload(
            List<@NotBlank @Size(max = 64) String> allowedAppCodes,
            List<@NotBlank @Size(max = 128) String> allowedSubjectIds,
            List<@NotBlank @Size(max = 64) String> requiredRoles
    ) {

        public DataServiceDefinition.PermissionBoundary toDomain() {
            return new DataServiceDefinition.PermissionBoundary(
                    allowedAppCodes == null ? List.of() : allowedAppCodes,
                    allowedSubjectIds == null ? List.of() : allowedSubjectIds,
                    requiredRoles == null ? List.of() : requiredRoles
            );
        }
    }

    public record CachePolicyPayload(
            Boolean enabled,
            @Positive Long ttlSeconds,
            @Size(max = 256) String cacheKeyTemplate,
            DataServiceDefinition.CacheScope scope,
            Boolean cacheNullValue,
            List<@NotBlank @Size(max = 128) String> invalidationEvents
    ) {

        public DataServiceDefinition.CachePolicy toDomain() {
            return new DataServiceDefinition.CachePolicy(
                    Boolean.TRUE.equals(enabled),
                    ttlSeconds,
                    cacheKeyTemplate,
                    scope,
                    Boolean.TRUE.equals(cacheNullValue),
                    invalidationEvents == null ? List.of() : invalidationEvents
            );
        }
    }

    public record ParameterPayload(
            UUID parameterId,
            @NotBlank @Size(max = 64) String paramCode,
            @NotNull ServiceParameterDefinition.ParameterType paramType,
            Boolean required,
            @Size(max = 512) String defaultValue,
            @Valid ValidationRulePayload validationRule,
            Boolean enabled,
            @Size(max = 512) String description,
            @PositiveOrZero Integer sortOrder
    ) {

        public DataServiceDefinitionCommands.ParameterCommand toCommand() {
            return new DataServiceDefinitionCommands.ParameterCommand(
                    parameterId,
                    paramCode,
                    paramType,
                    Boolean.TRUE.equals(required),
                    defaultValue,
                    validationRule == null ? null : validationRule.toDomain(),
                    enabled == null || enabled,
                    description,
                    sortOrder == null ? 0 : sortOrder
            );
        }
    }

    public record ValidationRulePayload(
            @PositiveOrZero Integer minLength,
            @PositiveOrZero Integer maxLength,
            BigDecimal minValue,
            BigDecimal maxValue,
            @Size(max = 256) String regex,
            List<@NotBlank @Size(max = 128) String> allowedValues,
            @Positive Integer maxPageSize
    ) {

        public ServiceParameterDefinition.ValidationRule toDomain() {
            return new ServiceParameterDefinition.ValidationRule(
                    minLength,
                    maxLength,
                    minValue,
                    maxValue,
                    regex,
                    allowedValues == null ? List.of() : allowedValues,
                    maxPageSize
            );
        }
    }

    public record FieldMappingPayload(
            UUID mappingId,
            @NotBlank @Size(max = 128) String sourceField,
            @NotBlank @Size(max = 128) String targetField,
            @Valid TransformRulePayload transformRule,
            Boolean masked,
            @Size(max = 512) String description,
            @PositiveOrZero Integer sortOrder
    ) {

        public DataServiceDefinitionCommands.FieldMappingCommand toCommand() {
            return new DataServiceDefinitionCommands.FieldMappingCommand(
                    mappingId,
                    sourceField,
                    targetField,
                    transformRule == null ? null : transformRule.toDomain(),
                    Boolean.TRUE.equals(masked),
                    description,
                    sortOrder == null ? 0 : sortOrder
            );
        }
    }

    public record TransformRulePayload(
            ServiceFieldMapping.TransformType type,
            @Size(max = 512) String expression,
            @Size(max = 128) String formatPattern,
            @Size(max = 512) String constantValue
    ) {

        public ServiceFieldMapping.TransformRule toDomain() {
            return new ServiceFieldMapping.TransformRule(type, expression, formatPattern, constantValue);
        }
    }

    public record SummaryResponse(
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

    public record DetailResponse(
            UUID serviceId,
            UUID tenantId,
            String code,
            String name,
            DataServiceDefinition.ServiceType serviceType,
            DataServiceDefinition.SourceMode sourceMode,
            DataServiceDefinition.PermissionMode permissionMode,
            PermissionBoundaryResponse permissionBoundary,
            CachePolicyResponse cachePolicy,
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
            List<ParameterResponse> parameters,
            List<FieldMappingResponse> fieldMappings
    ) {
    }

    public record PermissionBoundaryResponse(
            List<String> allowedAppCodes,
            List<String> allowedSubjectIds,
            List<String> requiredRoles
    ) {
    }

    public record CachePolicyResponse(
            boolean enabled,
            Long ttlSeconds,
            String cacheKeyTemplate,
            DataServiceDefinition.CacheScope scope,
            boolean cacheNullValue,
            List<String> invalidationEvents
    ) {
    }

    public record ParameterResponse(
            UUID parameterId,
            String paramCode,
            ServiceParameterDefinition.ParameterType paramType,
            boolean required,
            String defaultValue,
            ValidationRuleResponse validationRule,
            boolean enabled,
            String description,
            int sortOrder
    ) {
    }

    public record ValidationRuleResponse(
            Integer minLength,
            Integer maxLength,
            BigDecimal minValue,
            BigDecimal maxValue,
            String regex,
            List<String> allowedValues,
            Integer maxPageSize
    ) {
    }

    public record FieldMappingResponse(
            UUID mappingId,
            String sourceField,
            String targetField,
            TransformRuleResponse transformRule,
            boolean masked,
            String description,
            int sortOrder
    ) {
    }

    public record TransformRuleResponse(
            ServiceFieldMapping.TransformType type,
            String expression,
            String formatPattern,
            String constantValue
    ) {
    }
}
