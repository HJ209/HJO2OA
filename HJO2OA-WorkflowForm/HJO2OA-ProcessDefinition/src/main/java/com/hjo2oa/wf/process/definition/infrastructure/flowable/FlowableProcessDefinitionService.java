package com.hjo2oa.wf.process.definition.infrastructure.flowable;

import com.hjo2oa.wf.process.definition.application.ProcessDefinitionEngineGateway;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinition;
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

    public FlowableProcessDefinitionService(RepositoryService repositoryService) {
        this.repositoryService = Objects.requireNonNull(repositoryService, "repositoryService must not be null");
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
        String processId = xmlId(definition.code());
        String startEvent = xmlId(defaultText(definition.startNodeId(), "start"));
        String userTask = xmlId(defaultText(definition.startNodeId(), "submit"));
        String endEvent = xmlId(defaultText(definition.endNodeId(), "end"));
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
                    <userTask id="%s" name="%s" flowable:assignee="${initiatorId}"/>
                    <sequenceFlow id="flow_task_to_end" sourceRef="%s" targetRef="%s"/>
                    <endEvent id="%s" name="End"/>
                  </process>
                </definitions>
                """.formatted(
                StandardCharsets.UTF_8.name(),
                processId,
                name,
                "start".equals(startEvent) ? "start_event" : startEvent + "_event",
                "start".equals(startEvent) ? "start_event" : startEvent + "_event",
                userTask,
                userTask,
                name,
                userTask,
                "end".equals(endEvent) ? "end_event" : endEvent + "_event",
                "end".equals(endEvent) ? "end_event" : endEvent + "_event"
        );
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
