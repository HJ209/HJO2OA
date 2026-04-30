package com.hjo2oa.wf.process.definition.domain.model;

public record WorkflowRouteDefinition(
        String routeId,
        String name,
        String sourceNodeId,
        String targetNodeId,
        WorkflowRouteCondition condition,
        boolean defaultRoute,
        int sortOrder
) {
}
