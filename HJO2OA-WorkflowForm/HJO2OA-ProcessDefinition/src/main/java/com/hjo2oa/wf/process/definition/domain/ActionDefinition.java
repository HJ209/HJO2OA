package com.hjo2oa.wf.process.definition.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ActionDefinition(
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

    public ActionDefinition {
        Objects.requireNonNull(id, "id must not be null");
        code = requireText(code, "code");
        name = requireText(name, "name");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(routeTarget, "routeTarget must not be null");
        uiConfig = normalize(uiConfig);
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ActionDefinition create(
            UUID id,
            String code,
            String name,
            ActionCategory category,
            RouteTarget routeTarget,
            boolean requireOpinion,
            boolean requireTarget,
            String uiConfig,
            UUID tenantId,
            Instant now
    ) {
        return new ActionDefinition(
                id,
                code,
                name,
                category,
                routeTarget,
                requireOpinion,
                requireTarget,
                uiConfig,
                tenantId,
                now,
                now
        );
    }

    public ActionDefinition update(
            String name,
            ActionCategory category,
            RouteTarget routeTarget,
            boolean requireOpinion,
            boolean requireTarget,
            String uiConfig,
            Instant now
    ) {
        return new ActionDefinition(
                id,
                code,
                name,
                category,
                routeTarget,
                requireOpinion,
                requireTarget,
                uiConfig,
                tenantId,
                createdAt,
                now
        );
    }

    public ActionDefinitionView toView() {
        return new ActionDefinitionView(
                id,
                code,
                name,
                category,
                routeTarget,
                requireOpinion,
                requireTarget,
                uiConfig,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
