package com.hjo2oa.data.service.interfaces;

import com.hjo2oa.data.service.application.DataServiceInvocationCommands;
import com.hjo2oa.data.service.domain.DataServiceDefinition;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class DataServiceInvocationDtos {

    private DataServiceInvocationDtos() {
    }

    public record InvocationRequest(
            @Size(max = 64) String appCode,
            @Size(max = 128) String subjectId,
            Map<String, Object> parameters
    ) {

        public DataServiceInvocationCommands.InvocationCommand toCommand(
                String serviceCode,
                String idempotencyKey
        ) {
            return new DataServiceInvocationCommands.InvocationCommand(
                    serviceCode,
                    appCode,
                    subjectId,
                    idempotencyKey,
                    parameters
            );
        }
    }

    public record ExecutionPlanResponse(
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
            java.util.List<DataServiceDefinitionDtos.FieldMappingResponse> outputMappings,
            java.util.List<String> outputFields,
            boolean openApiReusable,
            boolean reportReusable,
            Instant preparedAt
    ) {
    }
}
