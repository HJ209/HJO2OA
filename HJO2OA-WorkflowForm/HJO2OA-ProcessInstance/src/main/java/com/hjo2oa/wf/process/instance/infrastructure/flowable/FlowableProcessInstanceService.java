package com.hjo2oa.wf.process.instance.infrastructure.flowable;

import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceEngineGateway;
import com.hjo2oa.wf.process.instance.domain.ProcessInstance;
import com.hjo2oa.wf.process.instance.domain.TaskInstance;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "hjo2oa.workflow.engine", havingValue = "flowable", matchIfMissing = true)
public class FlowableProcessInstanceService implements ProcessInstanceEngineGateway {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public FlowableProcessInstanceService(
            RepositoryService repositoryService,
            RuntimeService runtimeService,
            TaskService taskService
    ) {
        this.repositoryService = Objects.requireNonNull(repositoryService, "repositoryService must not be null");
        this.runtimeService = Objects.requireNonNull(runtimeService, "runtimeService must not be null");
        this.taskService = Objects.requireNonNull(taskService, "taskService must not be null");
    }

    @Override
    public void start(ProcessInstance instance, ProcessInstanceCommands.StartProcessCommand command) {
        Objects.requireNonNull(instance, "instance must not be null");
        Objects.requireNonNull(command, "command must not be null");
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(instance.definitionCode())
                .processDefinitionVersion(instance.definitionVersion())
                .processDefinitionTenantId(instance.tenantId().toString())
                .singleResult();
        if (processDefinition == null) {
            return;
        }
        runtimeService.startProcessInstanceById(
                processDefinition.getId(),
                instance.id().toString(),
                startVariables(instance, command)
        );
    }

    @Override
    public void claim(TaskInstance task, ProcessInstanceCommands.ClaimTaskCommand command) {
        findActiveTask(task).ifPresent(flowableTask ->
                taskService.claim(flowableTask.getId(), command.assigneeId().toString()));
    }

    @Override
    public void transfer(TaskInstance task, ProcessInstanceCommands.TransferTaskCommand command) {
        findActiveTask(task).ifPresent(flowableTask ->
                taskService.setAssignee(flowableTask.getId(), command.toAssigneeId().toString()));
    }

    @Override
    public void complete(
            ProcessInstance instance,
            TaskInstance task,
            ProcessInstanceCommands.CompleteTaskCommand command,
            Map<String, Object> variables
    ) {
        findActiveTask(task).ifPresent(flowableTask ->
                taskService.complete(flowableTask.getId(), completeVariables(instance, task, command, variables)));
    }

    @Override
    public void terminate(ProcessInstance instance, ProcessInstanceCommands.TerminateProcessCommand command) {
        runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(instance.id().toString())
                .active()
                .list()
                .forEach(processInstance ->
                        runtimeService.deleteProcessInstance(processInstance.getId(), command.reason()));
    }

    private java.util.Optional<Task> findActiveTask(TaskInstance task) {
        return taskService.createTaskQuery()
                .processInstanceBusinessKey(task.instanceId().toString())
                .taskDefinitionKey(task.nodeId())
                .active()
                .list()
                .stream()
                .findFirst();
    }

    private Map<String, Object> startVariables(
            ProcessInstance instance,
            ProcessInstanceCommands.StartProcessCommand command
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("domainInstanceId", instance.id().toString());
        variables.put("definitionId", instance.definitionId().toString());
        variables.put("definitionVersion", instance.definitionVersion());
        variables.put("definitionCode", instance.definitionCode());
        variables.put("businessKey", instance.businessKey());
        variables.put("title", instance.title());
        variables.put("initiatorId", instance.initiatorId().toString());
        variables.put("tenantId", instance.tenantId().toString());
        variables.put("formMetadataId", instance.formMetadataId().toString());
        variables.put("formDataId", instance.formDataId().toString());
        if (command.variables() != null) {
            variables.putAll(command.variables());
        }
        return variables;
    }

    private Map<String, Object> completeVariables(
            ProcessInstance instance,
            TaskInstance task,
            ProcessInstanceCommands.CompleteTaskCommand command,
            Map<String, Object> formDataPatch
    ) {
        Map<String, Object> variables = new HashMap<>();
        if (formDataPatch != null) {
            variables.putAll(formDataPatch);
        }
        variables.put("domainInstanceId", instance.id().toString());
        variables.put("domainTaskId", task.id().toString());
        variables.put("actionCode", command.actionCode());
        variables.put("actionName", command.actionName());
        variables.put("operatorId", command.operatorId().toString());
        variables.put("opinion", command.opinion());
        return variables;
    }
}
