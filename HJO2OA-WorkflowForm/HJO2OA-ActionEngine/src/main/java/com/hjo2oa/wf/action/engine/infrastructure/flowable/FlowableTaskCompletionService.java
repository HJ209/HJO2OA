package com.hjo2oa.wf.action.engine.infrastructure.flowable;

import com.hjo2oa.wf.action.engine.application.TaskCompletionGateway;
import com.hjo2oa.wf.action.engine.domain.ActionCategory;
import com.hjo2oa.wf.action.engine.domain.ActionDefinition;
import com.hjo2oa.wf.action.engine.domain.ActionExecutionRequest;
import com.hjo2oa.wf.action.engine.domain.TaskInstanceSnapshot;
import com.hjo2oa.wf.action.engine.domain.TaskStatus;
import java.util.HashMap;
import java.util.Map;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "hjo2oa.workflow.engine", havingValue = "flowable", matchIfMissing = true)
public class FlowableTaskCompletionService implements TaskCompletionGateway {

    private final TaskService taskService;

    public FlowableTaskCompletionService(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void apply(
            TaskInstanceSnapshot task,
            ActionDefinition definition,
            ActionExecutionRequest request,
            TaskStatus status
    ) {
        Task flowableTask = taskService.createTaskQuery()
                .taskId(task.taskId().toString())
                .active()
                .singleResult();
        if (flowableTask == null) {
            return;
        }
        if (definition.category() == ActionCategory.TRANSFER || definition.category() == ActionCategory.DELEGATE) {
            taskService.setAssignee(flowableTask.getId(), firstAssignee(request));
            return;
        }
        if (status == TaskStatus.COMPLETED) {
            taskService.complete(flowableTask.getId(), variables(task, definition, request));
        }
    }

    private Map<String, Object> variables(
            TaskInstanceSnapshot task,
            ActionDefinition definition,
            ActionExecutionRequest request
    ) {
        Map<String, Object> variables = new HashMap<>(request.formDataPatch());
        variables.put("domainTaskId", task.taskId().toString());
        variables.put("actionCode", definition.code());
        variables.put("actionCategory", definition.category().name());
        variables.put("operatorId", request.operator().personId());
        variables.put("opinion", request.opinion());
        return variables;
    }

    private String firstAssignee(ActionExecutionRequest request) {
        return request.targetAssigneeIds().isEmpty() ? null : request.targetAssigneeIds().get(0);
    }
}
