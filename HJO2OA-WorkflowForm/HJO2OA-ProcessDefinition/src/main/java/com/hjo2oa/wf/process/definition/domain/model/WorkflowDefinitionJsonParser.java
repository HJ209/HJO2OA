package com.hjo2oa.wf.process.definition.domain.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class WorkflowDefinitionJsonParser {

    private final ObjectMapper objectMapper;

    public WorkflowDefinitionJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public WorkflowDefinitionModel parse(String nodesJson, String routesJson) {
        return new WorkflowDefinitionModel(parseNodes(read(nodesJson)), parseRoutes(read(routesJson)));
    }

    private List<WorkflowNodeDefinition> parseNodes(JsonNode root) {
        JsonNode array = root.isArray() ? root : root.path("nodes");
        if (!array.isArray()) {
            return List.of();
        }
        List<WorkflowNodeDefinition> nodes = new ArrayList<>();
        for (JsonNode node : array) {
            String nodeId = firstText(node, "nodeId", "id", "key");
            if (nodeId == null || nodeId.isBlank()) {
                continue;
            }
            WorkflowParticipantRule participantRule = parseParticipantRule(
                    firstObject(node, "participantRule", "participant", "assigneeRule")
            );
            nodes.add(new WorkflowNodeDefinition(
                    nodeId.trim(),
                    defaultText(firstText(node, "name", "label", "title"), nodeId.trim()),
                    defaultText(firstText(node, "type", "nodeType"), "USER_TASK").trim().toUpperCase(),
                    participantRule,
                    readStringArray(firstArray(node, "actionCodes", "actions")),
                    firstText(firstObject(node, "multiInstance", "multiInstanceRule"), "type"),
                    firstText(firstObject(node, "multiInstance", "multiInstanceRule"), "completionCondition"),
                    readObject(node)
            ));
        }
        return List.copyOf(nodes);
    }

    private List<WorkflowRouteDefinition> parseRoutes(JsonNode root) {
        JsonNode array = root.isArray() ? root : root.path("routes");
        if (!array.isArray()) {
            return List.of();
        }
        List<WorkflowRouteDefinition> routes = new ArrayList<>();
        int index = 0;
        for (JsonNode route : array) {
            String source = firstText(route, "sourceNodeId", "source", "from");
            String target = firstText(route, "targetNodeId", "target", "to");
            if (source == null || target == null || source.isBlank() || target.isBlank()) {
                continue;
            }
            WorkflowRouteCondition condition = parseCondition(firstObject(route, "condition", "rule"));
            routes.add(new WorkflowRouteDefinition(
                    defaultText(firstText(route, "routeId", "id", "key"), "route-" + index),
                    firstText(route, "name", "label"),
                    source.trim(),
                    target.trim(),
                    condition,
                    firstBoolean(route, "defaultRoute", "isDefault"),
                    firstInt(route, index, "sortOrder", "order")
            ));
            index++;
        }
        return List.copyOf(routes);
    }

    private WorkflowParticipantRule parseParticipantRule(JsonNode rule) {
        if (rule == null || rule.isMissingNode() || rule.isNull()) {
            return null;
        }
        List<String> ids = readStringArray(firstArray(rule, "ids", "refIds", "candidateIds", "personIds"));
        String refId = firstText(rule, "refId", "id", "organizationId", "departmentId", "positionId", "roleId", "personId");
        if (refId != null && ids.isEmpty()) {
            ids = List.of(refId);
        }
        return new WorkflowParticipantRule(
                defaultText(firstText(rule, "type", "participantType"), "SPECIFIC_PERSON").trim().toUpperCase(),
                ids,
                refId,
                firstText(rule, "refFieldCode", "field", "fieldCode"),
                firstText(rule, "expression"),
                readObject(rule)
        );
    }

    private WorkflowRouteCondition parseCondition(JsonNode condition) {
        if (condition == null || condition.isMissingNode() || condition.isNull()) {
            return null;
        }
        return new WorkflowRouteCondition(
                firstText(condition, "field", "fieldCode"),
                firstText(condition, "operator", "op"),
                firstText(condition, "expectedValue", "value"),
                firstText(condition, "expression")
        );
    }

    private JsonNode read(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createArrayNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid workflow definition JSON", ex);
        }
    }

    private Map<String, Object> readObject(JsonNode node) {
        Map<String, Object> value = objectMapper.convertValue(node, LinkedHashMap.class);
        return value == null ? Map.of() : Map.copyOf(value);
    }

    private List<String> readStringArray(JsonNode array) {
        if (array == null || !array.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText().trim());
            } else if (item.isObject()) {
                String value = firstText(item, "id", "code", "value");
                if (value != null && !value.isBlank()) {
                    values.add(value.trim());
                }
            }
        }
        return List.copyOf(values);
    }

    private JsonNode firstObject(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isObject()) {
                return value;
            }
        }
        return null;
    }

    private JsonNode firstArray(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private String firstText(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
            if (value.isNumber() || value.isBoolean()) {
                return value.asText();
            }
        }
        return null;
    }

    private boolean firstBoolean(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isBoolean()) {
                return value.asBoolean();
            }
        }
        return false;
    }

    private int firstInt(JsonNode node, int defaultValue, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isInt()) {
                return value.asInt();
            }
        }
        return defaultValue;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
