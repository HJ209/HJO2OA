package com.hjo2oa.wf.process.definition.domain.model;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record WorkflowDefinitionModel(
        List<WorkflowNodeDefinition> nodes,
        List<WorkflowRouteDefinition> routes
) {

    public Optional<WorkflowNodeDefinition> findNode(String nodeId) {
        if (nodeId == null) {
            return Optional.empty();
        }
        return nodes.stream()
                .filter(node -> nodeId.equals(node.nodeId()))
                .findFirst();
    }

    public Optional<WorkflowNodeDefinition> startNode() {
        return nodes.stream().filter(WorkflowNodeDefinition::isStart).findFirst();
    }

    public List<WorkflowNodeDefinition> endNodes() {
        return nodes.stream().filter(WorkflowNodeDefinition::isEnd).toList();
    }

    public List<WorkflowRouteDefinition> outgoingRoutes(String nodeId) {
        return routes.stream()
                .filter(route -> nodeId != null && nodeId.equals(route.sourceNodeId()))
                .sorted(Comparator.comparingInt(WorkflowRouteDefinition::sortOrder))
                .toList();
    }
}
