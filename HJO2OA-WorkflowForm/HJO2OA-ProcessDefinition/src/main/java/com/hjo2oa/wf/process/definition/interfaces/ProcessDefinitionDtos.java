package com.hjo2oa.wf.process.definition.interfaces;

import com.hjo2oa.wf.process.definition.application.ProcessDefinitionCommands;
import com.hjo2oa.wf.process.definition.domain.ActionCategory;
import com.hjo2oa.wf.process.definition.domain.DefinitionStatus;
import com.hjo2oa.wf.process.definition.domain.RouteTarget;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ProcessDefinitionDtos {

    private ProcessDefinitionDtos() {
    }

    public record SaveDefinitionRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @Size(max = 64) String category,
            UUID formMetadataId,
            @Size(max = 64) String startNodeId,
            @Size(max = 64) String endNodeId,
            @NotNull JsonNode nodes,
            @NotNull JsonNode routes,
            UUID tenantId
    ) {

        public ProcessDefinitionCommands.SaveDefinitionCommand toCommand(
                UUID definitionId,
                UUID resolvedTenantId,
                String idempotencyKey,
                String requestId
        ) {
            return new ProcessDefinitionCommands.SaveDefinitionCommand(
                    definitionId,
                    code,
                    name,
                    category,
                    formMetadataId,
                    startNodeId,
                    endNodeId,
                    nodes.toString(),
                    routes.toString(),
                    resolvedTenantId == null ? tenantId : resolvedTenantId,
                    idempotencyKey,
                    requestId
            );
        }
    }

    public record PublishDefinitionRequest(
            UUID publishedBy
    ) {

        public ProcessDefinitionCommands.PublishDefinitionCommand toCommand(
                UUID definitionId,
                String idempotencyKey,
                String requestId
        ) {
            return new ProcessDefinitionCommands.PublishDefinitionCommand(definitionId, publishedBy, idempotencyKey, requestId);
        }
    }

    public record DefinitionResponse(
            UUID id,
            String code,
            String name,
            String category,
            int version,
            DefinitionStatus status,
            UUID formMetadataId,
            String startNodeId,
            String endNodeId,
            String nodes,
            String routes,
            UUID tenantId,
            Instant publishedAt,
            UUID publishedBy,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record SaveActionRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @NotNull ActionCategory category,
            @NotNull RouteTarget routeTarget,
            boolean requireOpinion,
            boolean requireTarget,
            JsonNode uiConfig,
            UUID tenantId
    ) {

        public ProcessDefinitionCommands.SaveActionCommand toCommand(
                UUID actionId,
                UUID resolvedTenantId,
                String idempotencyKey,
                String requestId
        ) {
            return new ProcessDefinitionCommands.SaveActionCommand(
                    actionId,
                    code,
                    name,
                    category,
                    routeTarget,
                    requireOpinion,
                    requireTarget,
                    uiConfig == null ? null : uiConfig.toString(),
                    resolvedTenantId == null ? tenantId : resolvedTenantId,
                    idempotencyKey,
                    requestId
            );
        }
    }

    public record ActionResponse(
            UUID id,
            String code,
            String name,
            ActionCategory category,
            RouteTarget routeTarget,
            boolean requireOpinion,
            boolean requireTarget,
            String uiConfig,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
