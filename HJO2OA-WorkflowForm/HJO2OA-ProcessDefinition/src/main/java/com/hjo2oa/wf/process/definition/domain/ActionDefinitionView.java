package com.hjo2oa.wf.process.definition.domain;

import java.time.Instant;
import java.util.UUID;

public record ActionDefinitionView(
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
