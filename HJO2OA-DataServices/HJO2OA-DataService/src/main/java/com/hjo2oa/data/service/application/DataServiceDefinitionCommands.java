package com.hjo2oa.data.service.application;

import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.domain.ServiceFieldMapping;
import com.hjo2oa.data.service.domain.ServiceParameterDefinition;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class DataServiceDefinitionCommands {

    private DataServiceDefinitionCommands() {
    }

    public record ListQuery(
            int page,
            int size,
            String code,
            String keyword,
            DataServiceDefinition.ServiceType serviceType,
            DataServiceDefinition.Status status
    ) {

        public ListQuery {
            if (page < 1) {
                throw new IllegalArgumentException("page must be greater than 0");
            }
            if (size < 1) {
                throw new IllegalArgumentException("size must be greater than 0");
            }
        }
    }

    public record CreateCommand(
            UUID serviceId,
            String code,
            String name,
            DataServiceDefinition.ServiceType serviceType,
            DataServiceDefinition.SourceMode sourceMode,
            DataServiceDefinition.PermissionMode permissionMode,
            DataServiceDefinition.PermissionBoundary permissionBoundary,
            DataServiceDefinition.CachePolicy cachePolicy,
            String sourceRef,
            String connectorId,
            String description,
            List<ParameterCommand> parameters,
            List<FieldMappingCommand> fieldMappings
    ) {

        public CreateCommand {
            Objects.requireNonNull(serviceId, "serviceId must not be null");
            parameters = parameters == null ? List.of() : List.copyOf(parameters);
            fieldMappings = fieldMappings == null ? List.of() : List.copyOf(fieldMappings);
        }
    }

    public record UpdateCommand(
            UUID serviceId,
            String code,
            String name,
            DataServiceDefinition.ServiceType serviceType,
            DataServiceDefinition.SourceMode sourceMode,
            DataServiceDefinition.PermissionMode permissionMode,
            DataServiceDefinition.PermissionBoundary permissionBoundary,
            DataServiceDefinition.CachePolicy cachePolicy,
            String sourceRef,
            String connectorId,
            String description,
            List<ParameterCommand> parameters,
            List<FieldMappingCommand> fieldMappings
    ) {

        public UpdateCommand {
            Objects.requireNonNull(serviceId, "serviceId must not be null");
            parameters = parameters == null ? null : List.copyOf(parameters);
            fieldMappings = fieldMappings == null ? null : List.copyOf(fieldMappings);
        }
    }

    public record ParameterCommand(
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

    public record FieldMappingCommand(
            UUID mappingId,
            String sourceField,
            String targetField,
            ServiceFieldMapping.TransformRule transformRule,
            boolean masked,
            String description,
            int sortOrder
    ) {
    }
}
