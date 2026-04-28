package com.hjo2oa.wf.action.engine.application;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import com.hjo2oa.wf.action.engine.domain.ActionCategory;
import com.hjo2oa.wf.action.engine.domain.ActionDefinition;
import com.hjo2oa.wf.action.engine.domain.ActionDefinitionRepository;
import com.hjo2oa.wf.action.engine.domain.ActionExecutionRequest;
import com.hjo2oa.wf.action.engine.domain.ActionExecutionResult;
import com.hjo2oa.wf.action.engine.domain.ActionResultStatus;
import com.hjo2oa.wf.action.engine.domain.ProcessTaskActionExecutedEvent;
import com.hjo2oa.wf.action.engine.domain.ProcessTaskCompletedEvent;
import com.hjo2oa.wf.action.engine.domain.ProcessTaskTerminatedEvent;
import com.hjo2oa.wf.action.engine.domain.ProcessTaskTransferredEvent;
import com.hjo2oa.wf.action.engine.domain.TaskAction;
import com.hjo2oa.wf.action.engine.domain.TaskActionRepository;
import com.hjo2oa.wf.action.engine.domain.TaskInstanceGateway;
import com.hjo2oa.wf.action.engine.domain.TaskInstanceSnapshot;
import com.hjo2oa.wf.action.engine.domain.TaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ActionEngineApplicationService {

    private final TaskInstanceGateway taskInstanceGateway;
    private final ActionDefinitionRepository actionDefinitionRepository;
    private final TaskActionRepository taskActionRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    @Autowired
    public ActionEngineApplicationService(
            TaskInstanceGateway taskInstanceGateway,
            ActionDefinitionRepository actionDefinitionRepository,
            TaskActionRepository taskActionRepository,
            DomainEventPublisher domainEventPublisher
    ) {
        this(taskInstanceGateway, actionDefinitionRepository, taskActionRepository, domainEventPublisher, Clock.systemUTC());
    }
    public ActionEngineApplicationService(
            TaskInstanceGateway taskInstanceGateway,
            ActionDefinitionRepository actionDefinitionRepository,
            TaskActionRepository taskActionRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.taskInstanceGateway = taskInstanceGateway;
        this.actionDefinitionRepository = actionDefinitionRepository;
        this.taskActionRepository = taskActionRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    public List<ActionDefinition> availableActions(UUID taskId) {
        TaskInstanceSnapshot task = loadPendingTask(taskId);
        return actionDefinitionRepository.findAvailableActions(task);
    }

    public ActionExecutionResult approve(ActionEngineCommands.ExecuteActionCommand command) {
        return execute(command, ActionCategory.APPROVE);
    }

    public ActionExecutionResult reject(ActionEngineCommands.ExecuteActionCommand command) {
        return execute(command, ActionCategory.REJECT);
    }

    public ActionExecutionResult transfer(ActionEngineCommands.ExecuteActionCommand command) {
        return execute(command, ActionCategory.TRANSFER);
    }

    public ActionExecutionResult addSign(ActionEngineCommands.ExecuteActionCommand command) {
        return execute(command, ActionCategory.ADD_SIGN);
    }

    public ActionExecutionResult reduceSign(ActionEngineCommands.ExecuteActionCommand command) {
        return execute(command, ActionCategory.REDUCE_SIGN);
    }

    public ActionExecutionResult execute(ActionEngineCommands.ExecuteActionCommand command) {
        return execute(command, null);
    }

    public List<TaskAction> records(UUID taskId) {
        return taskActionRepository.findByTaskId(taskId);
    }

    private ActionExecutionResult execute(
            ActionEngineCommands.ExecuteActionCommand command,
            ActionCategory expectedCategory
    ) {
        TaskInstanceSnapshot task = loadPendingTask(command.taskId());
        ActionExecutionRequest request = command.toExecutionRequest();
        ActionDefinition definition = actionDefinitionRepository
                .findAvailableAction(task, request.actionCode())
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.CONFLICT,
                        "Process action is not available for current task"
                ));
        if (expectedCategory != null && definition.category() != expectedCategory) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Process action category mismatch");
        }
        validateRequest(definition, request);

        return taskActionRepository.findByIdempotency(task.taskId(), definition.code(), request.idempotencyKey())
                .map(existing -> new ActionExecutionResult(existing, task.status()))
                .orElseGet(() -> executeNewAction(task, definition, request));
    }

    private ActionExecutionResult executeNewAction(
            TaskInstanceSnapshot task,
            ActionDefinition definition,
            ActionExecutionRequest request
    ) {
        Instant now = clock.instant();
        TaskStatus nextStatus = updateTask(task, definition, request);
        TaskAction action = taskActionRepository.save(
                TaskAction.create(task, definition, request, ActionResultStatus.SUCCESS, now)
        );
        publishTaskEvents(task, action, nextStatus, now);
        return new ActionExecutionResult(action, nextStatus);
    }

    private TaskStatus updateTask(
            TaskInstanceSnapshot task,
            ActionDefinition definition,
            ActionExecutionRequest request
    ) {
        return switch (definition.category()) {
            case APPROVE -> taskInstanceGateway.updateStatus(task.taskId(), TaskStatus.COMPLETED).status();
            case REJECT, RETURN, TERMINATE -> taskInstanceGateway.updateStatus(task.taskId(), TaskStatus.REJECTED).status();
            case TRANSFER, DELEGATE -> taskInstanceGateway.transfer(task.taskId(), firstAssignee(request)).status();
            case ADD_SIGN -> taskInstanceGateway.addSign(task.taskId(), firstAssignee(request)).status();
            case REDUCE_SIGN -> taskInstanceGateway.reduceSign(task.taskId(), firstAssignee(request)).status();
            case SUSPEND, CUSTOM -> taskInstanceGateway.updateStatus(task.taskId(), TaskStatus.COMPLETED).status();
        };
    }

    private void publishTaskEvents(
            TaskInstanceSnapshot before,
            TaskAction action,
            TaskStatus taskStatus,
            Instant occurredAt
    ) {
        if (taskStatus == TaskStatus.COMPLETED) {
            domainEventPublisher.publish(ProcessTaskCompletedEvent.from(action, occurredAt));
        } else if (taskStatus == TaskStatus.TRANSFERRED) {
            domainEventPublisher.publish(ProcessTaskTransferredEvent.from(action, before.assigneeId(), occurredAt));
        } else if (taskStatus == TaskStatus.REJECTED || taskStatus == TaskStatus.TERMINATED) {
            domainEventPublisher.publish(ProcessTaskTerminatedEvent.from(action, occurredAt));
        }
        domainEventPublisher.publish(ProcessTaskActionExecutedEvent.from(action, taskStatus, occurredAt));
    }

    private TaskInstanceSnapshot loadPendingTask(UUID taskId) {
        TaskInstanceSnapshot task = taskInstanceGateway.findById(taskId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Process task not found"));
        if (!task.pending()) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Process task status is not pending");
        }
        return task;
    }

    private void validateRequest(ActionDefinition definition, ActionExecutionRequest request) {
        if (definition.requireOpinion() && !request.hasOpinion()) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Process action opinion is required");
        }
        if (definition.requireTarget() && !request.hasTarget()) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Process action target is required");
        }
        if (requiresAssignee(definition.category()) && request.targetAssigneeIds().isEmpty()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "Process action target assignee is required"
            );
        }
    }

    private boolean requiresAssignee(ActionCategory category) {
        return category == ActionCategory.TRANSFER
                || category == ActionCategory.DELEGATE
                || category == ActionCategory.ADD_SIGN
                || category == ActionCategory.REDUCE_SIGN;
    }

    private String firstAssignee(ActionExecutionRequest request) {
        if (request.targetAssigneeIds().isEmpty()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "Process action target assignee is required"
            );
        }
        return request.targetAssigneeIds().get(0);
    }
}
