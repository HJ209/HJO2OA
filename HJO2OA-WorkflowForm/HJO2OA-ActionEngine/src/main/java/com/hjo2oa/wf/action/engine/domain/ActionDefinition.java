package com.hjo2oa.wf.action.engine.domain;

import java.util.Map;
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
        Map<String, Object> uiConfig,
        String tenantId
) {

    public ActionDefinition {
        Objects.requireNonNull(id, "id must not be null");
        code = requireText(code, "code");
        name = requireText(name, "name");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(routeTarget, "routeTarget must not be null");
        tenantId = requireText(tenantId, "tenantId");
        uiConfig = uiConfig == null ? Map.of() : Map.copyOf(uiConfig);
    }

    public static ActionDefinition preset(
            String code,
            String name,
            ActionCategory category,
            RouteTarget routeTarget,
            boolean requireOpinion,
            boolean requireTarget,
            String tenantId
    ) {
        return new ActionDefinition(
                UUID.nameUUIDFromBytes((tenantId + ":" + code).getBytes()),
                code,
                name,
                category,
                routeTarget,
                requireOpinion,
                requireTarget,
                Map.of(),
                tenantId
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
