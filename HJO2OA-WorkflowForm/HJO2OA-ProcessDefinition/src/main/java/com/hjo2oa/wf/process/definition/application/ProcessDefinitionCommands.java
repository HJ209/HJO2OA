package com.hjo2oa.wf.process.definition.application;

import com.hjo2oa.wf.process.definition.domain.ActionCategory;
import com.hjo2oa.wf.process.definition.domain.DefinitionStatus;
import com.hjo2oa.wf.process.definition.domain.RouteTarget;
import java.util.Objects;
import java.util.UUID;

public final class ProcessDefinitionCommands {

    private ProcessDefinitionCommands() {
    }

    public record SaveDefinitionCommand(
            UUID definitionId,
            String code,
            String name,
            String category,
            UUID formMetadataId,
            String startNodeId,
            String endNodeId,
            String nodes,
            String routes,
            UUID tenantId,
            String idempotencyKey,
            String requestId
    ) {
    }

    public record PublishDefinitionCommand(
            UUID definitionId,
            UUID publishedBy,
            String idempotencyKey,
            String requestId
    ) {

        public PublishDefinitionCommand {
            Objects.requireNonNull(definitionId, "definitionId must not be null");
        }
    }

    public record DefinitionQuery(
            UUID tenantId,
            String code,
            String category,
            DefinitionStatus status
    ) {
    }

    public record SaveActionCommand(
            UUID actionId,
            String code,
            String name,
            ActionCategory category,
            RouteTarget routeTarget,
            boolean requireOpinion,
            boolean requireTarget,
            String uiConfig,
            UUID tenantId,
            String idempotencyKey,
            String requestId
    ) {
    }

    public record ActionQuery(
            UUID tenantId,
            ActionCategory category
    ) {
    }
}
