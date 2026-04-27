package com.hjo2oa.wf.process.definition.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProcessDefinition(
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

    public ProcessDefinition {
        Objects.requireNonNull(id, "id must not be null");
        code = ActionDefinition.requireText(code, "code");
        name = ActionDefinition.requireText(name, "name");
        category = ActionDefinition.normalize(category);
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
        Objects.requireNonNull(status, "status must not be null");
        startNodeId = ActionDefinition.normalize(startNodeId);
        endNodeId = ActionDefinition.normalize(endNodeId);
        nodes = requireJsonText(nodes, "nodes");
        routes = requireJsonText(routes, "routes");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (status == DefinitionStatus.ACTIVE && publishedAt == null) {
            throw new IllegalArgumentException("publishedAt must not be null for active definition");
        }
    }

    public static ProcessDefinition create(
            UUID id,
            String code,
            String name,
            String category,
            int version,
            UUID formMetadataId,
            String startNodeId,
            String endNodeId,
            String nodes,
            String routes,
            UUID tenantId,
            Instant now
    ) {
        return new ProcessDefinition(
                id,
                code,
                name,
                category,
                version,
                DefinitionStatus.DRAFT,
                formMetadataId,
                startNodeId,
                endNodeId,
                nodes,
                routes,
                tenantId,
                null,
                null,
                now,
                now
        );
    }

    public ProcessDefinition updateDraft(
            String name,
            String category,
            UUID formMetadataId,
            String startNodeId,
            String endNodeId,
            String nodes,
            String routes,
            Instant now
    ) {
        ensureDraft();
        return new ProcessDefinition(
                id,
                code,
                name,
                category,
                version,
                status,
                formMetadataId,
                startNodeId,
                endNodeId,
                nodes,
                routes,
                tenantId,
                publishedAt,
                publishedBy,
                createdAt,
                now
        );
    }

    public ProcessDefinition publish(UUID publisherId, Instant now) {
        if (status == DefinitionStatus.ACTIVE) {
            return this;
        }
        if (status == DefinitionStatus.DEPRECATED) {
            throw new IllegalStateException("deprecated definition cannot be published");
        }
        return new ProcessDefinition(
                id,
                code,
                name,
                category,
                version,
                DefinitionStatus.ACTIVE,
                formMetadataId,
                startNodeId,
                endNodeId,
                nodes,
                routes,
                tenantId,
                now,
                publisherId,
                createdAt,
                now
        );
    }

    public ProcessDefinition deprecate(Instant now) {
        if (status == DefinitionStatus.DEPRECATED) {
            return this;
        }
        return new ProcessDefinition(
                id,
                code,
                name,
                category,
                version,
                DefinitionStatus.DEPRECATED,
                formMetadataId,
                startNodeId,
                endNodeId,
                nodes,
                routes,
                tenantId,
                publishedAt,
                publishedBy,
                createdAt,
                now
        );
    }

    public ProcessDefinition copyAsDraft(UUID newId, int newVersion, Instant now) {
        return create(
                newId,
                code,
                name,
                category,
                newVersion,
                formMetadataId,
                startNodeId,
                endNodeId,
                nodes,
                routes,
                tenantId,
                now
        );
    }

    public ProcessDefinitionView toView() {
        return new ProcessDefinitionView(
                id,
                code,
                name,
                category,
                version,
                status,
                formMetadataId,
                startNodeId,
                endNodeId,
                nodes,
                routes,
                tenantId,
                publishedAt,
                publishedBy,
                createdAt,
                updatedAt
        );
    }

    public void ensureDraft() {
        if (status != DefinitionStatus.DRAFT) {
            throw new IllegalStateException("only draft definition can be changed");
        }
    }

    private static String requireJsonText(String value, String fieldName) {
        String normalized = ActionDefinition.requireText(value, fieldName);
        if (!normalized.startsWith("{") && !normalized.startsWith("[")) {
            throw new IllegalArgumentException(fieldName + " must be JSON text");
        }
        return normalized;
    }
}
