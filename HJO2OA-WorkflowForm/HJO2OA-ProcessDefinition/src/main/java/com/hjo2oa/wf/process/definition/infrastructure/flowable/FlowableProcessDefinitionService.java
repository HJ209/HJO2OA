package com.hjo2oa.wf.process.definition.infrastructure.flowable;

import com.hjo2oa.wf.process.definition.application.ProcessDefinitionEngineGateway;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinition;
import com.hjo2oa.wf.process.definition.domain.model.WorkflowDefinitionJsonParser;
import com.hjo2oa.wf.process.definition.domain.model.WorkflowDefinitionModel;
import com.hjo2oa.wf.process.definition.domain.model.WorkflowNodeDefinition;
import com.hjo2oa.wf.process.definition.domain.model.WorkflowRouteDefinition;
import com.hjo2oa.wf.process.definition.domain.model.WorkflowRouteCondition;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "hjo2oa.workflow.engine", havingValue = "flowable", matchIfMissing = true)
public class FlowableProcessDefinitionService implements ProcessDefinitionEngineGateway {

    private static final String BPMN_SUFFIX = ".bpmn20.xml";

    private final RepositoryService repositoryService;
    private final WorkflowDefinitionJsonParser modelParser;

    public FlowableProcessDefinitionService(
            RepositoryService repositoryService,
            WorkflowDefinitionJsonParser modelParser
    ) {
        this.repositoryService = Objects.requireNonNull(repositoryService, "repositoryService must not be null");
        this.modelParser = Objects.requireNonNull(modelParser, "modelParser must not be null");
    }

    @Override
    public void deploy(ProcessDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        String resourceName = definition.code() + "-v" + definition.version() + BPMN_SUFFIX;
        String deploymentKey = deploymentKey(definition);
        deleteExistingDeployment(definition);
        repositoryService.createDeployment()
                .key(deploymentKey)
                .name(definition.name())
                .tenantId(definition.tenantId().toString())
                .addString(resourceName, toBpmnXml(definition))
                .deploy();
    }

    @Override
    public void delete(ProcessDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        deleteExistingDeployment(definition);
    }

    private void deleteExistingDeployment(ProcessDefinition definition) {
        repositoryService.createDeploymentQuery()
                .deploymentKey(deploymentKey(definition))
                .deploymentTenantId(definition.tenantId().toString())
                .list()
                .forEach(deployment -> repositoryService.deleteDeployment(deployment.getId(), true));
    }

    private String deploymentKey(ProcessDefinition definition) {
        return definition.code() + ":v" + definition.version() + ":" + definition.id();
    }

    private String toBpmnXml(ProcessDefinition definition) {
        WorkflowDefinitionModel model = modelParser.parse(definition.nodes(), definition.routes());
        if (model.nodes().isEmpty()) {
            return fallbackBpmnXml(definition);
        }
        StringBuilder nodesXml = new StringBuilder();
        for (WorkflowNodeDefinition node : model.nodes()) {
            nodesXml.append(toNodeXml(node));
        }
        StringBuilder flowsXml = new StringBuilder();
        int index = 0;
        for (WorkflowRouteDefinition route : model.routes()) {
            flowsXml.append(toSequenceFlowXml(route, index++));
        }
        if (flowsXml.isEmpty()) {
            flowsXml.append(fallbackSequenceFlows(model));
        }
        return """
                <?xml version="1.0" encoding="%s"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xmlns:flowable="http://flowable.org/bpmn"
                             targetNamespace="https://hjo2oa.com/workflow">
                  <process id="%s" name="%s" isExecutable="true">
                %s%s  </process>
                </definitions>
                """.formatted(
                StandardCharsets.UTF_8.name(),
                xmlId(definition.code()),
                escape(definition.name()),
                nodesXml,
                flowsXml
        );
    }

    private String fallbackBpmnXml(ProcessDefinition definition) {
        String processId = xmlId(definition.code());
        String startEvent = xmlId(defaultText(definition.startNodeId(), "start_event"));
        String userTask = xmlId("submit_task");
        String endEvent = xmlId(defaultText(definition.endNodeId(), "end_event"));
        String name = escape(definition.name());
        return """
                <?xml version="1.0" encoding="%s"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xmlns:flowable="http://flowable.org/bpmn"
                             targetNamespace="https://hjo2oa.com/workflow">
                  <process id="%s" name="%s" isExecutable="true">
                    <startEvent id="%s" name="Start"/>
                    <sequenceFlow id="flow_start_to_task" sourceRef="%s" targetRef="%s"/>
                    <userTask id="%s" name="%s"/>
                    <sequenceFlow id="flow_task_to_end" sourceRef="%s" targetRef="%s"/>
                    <endEvent id="%s" name="End"/>
                  </process>
                </definitions>
                """.formatted(
                StandardCharsets.UTF_8.name(),
                processId,
                name,
                startEvent,
                startEvent,
                userTask,
                userTask,
                name,
                userTask,
                endEvent,
                endEvent
        );
    }

    private String toNodeXml(WorkflowNodeDefinition node) {
        String id = xmlId(node.nodeId());
        String name = escape(node.name());
        if (node.isStart()) {
            return "    <startEvent id=\"%s\" name=\"%s\"/>\n".formatted(id, name);
        }
        if (node.isEnd()) {
            return "    <endEvent id=\"%s\" name=\"%s\"/>\n".formatted(id, name);
        }
        String type = node.type() == null ? "USER_TASK" : node.type().toUpperCase();
        return switch (type) {
            case "EXCLUSIVE_GATEWAY" -> "    <exclusiveGateway id=\"%s\" name=\"%s\"/>\n".formatted(id, name);
            case "PARALLEL_GATEWAY" -> "    <parallelGateway id=\"%s\" name=\"%s\"/>\n".formatted(id, name);
            case "INCLUSIVE_GATEWAY" -> "    <inclusiveGateway id=\"%s\" name=\"%s\"/>\n".formatted(id, name);
            case "SERVICE_TASK" -> "    <serviceTask id=\"%s\" name=\"%s\" flowable:class=\"com.hjo2oa.wf.process.definition.infrastructure.flowable.FlowableServiceTaskPassThroughDelegate\"/>\n"
                    .formatted(id, name);
            default -> "    <userTask id=\"%s\" name=\"%s\"/>\n".formatted(id, name);
        };
    }

    private String toSequenceFlowXml(WorkflowRouteDefinition route, int index) {
        String id = xmlId(defaultText(route.routeId(), "flow_" + index));
        StringBuilder builder = new StringBuilder("    <sequenceFlow id=\"%s\" sourceRef=\"%s\" targetRef=\"%s\""
                .formatted(id, xmlId(route.sourceNodeId()), xmlId(route.targetNodeId())));
        String condition = conditionExpression(route.condition());
        if (condition == null || route.defaultRoute()) {
            builder.append("/>\n");
            return builder.toString();
        }
        builder.append(">\n      <conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[")
                .append(condition)
                .append("]]></conditionExpression>\n    </sequenceFlow>\n");
        return builder.toString();
    }

    private String conditionExpression(WorkflowRouteCondition condition) {
        if (condition == null) {
            return null;
        }
        if (condition.expression() != null && !condition.expression().isBlank()) {
            return condition.expression();
        }
        if (condition.field() == null || condition.operator() == null) {
            return null;
        }
        String operator = switch (condition.operator().trim().toUpperCase(java.util.Locale.ROOT)) {
            case "NE", "!=" -> "!=";
            case "PRESENT" -> "!=";
            default -> "==";
        };
        String expected = "PRESENT".equalsIgnoreCase(condition.operator()) ? "null" : "'" + escape(condition.expectedValue()) + "'";
        return "${" + condition.field() + " " + operator + " " + expected + "}";
    }

    private String fallbackSequenceFlows(WorkflowDefinitionModel model) {
        WorkflowNodeDefinition start = model.startNode().orElse(null);
        WorkflowNodeDefinition firstTask = model.nodes().stream().filter(WorkflowNodeDefinition::isUserTask).findFirst().orElse(null);
        WorkflowNodeDefinition end = model.endNodes().stream().findFirst().orElse(null);
        if (start == null || firstTask == null || end == null) {
            return "";
        }
        return """
                    <sequenceFlow id="flow_start_to_first" sourceRef="%s" targetRef="%s"/>
                    <sequenceFlow id="flow_first_to_end" sourceRef="%s" targetRef="%s"/>
                """.formatted(xmlId(start.nodeId()), xmlId(firstTask.nodeId()), xmlId(firstTask.nodeId()), xmlId(end.nodeId()));
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String xmlId(String value) {
        String normalized = defaultText(value, "node").replaceAll("[^A-Za-z0-9_\\-]", "_");
        if (normalized.isBlank()) {
            return "node";
        }
        return Character.isLetter(normalized.charAt(0)) || normalized.charAt(0) == '_'
                ? normalized
                : "n_" + normalized;
    }

    private String escape(String value) {
        return defaultText(value, "").replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
