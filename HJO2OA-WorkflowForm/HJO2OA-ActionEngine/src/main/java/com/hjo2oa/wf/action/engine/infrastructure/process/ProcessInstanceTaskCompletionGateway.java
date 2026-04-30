package com.hjo2oa.wf.action.engine.infrastructure.process;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.wf.action.engine.application.TaskCompletionGateway;
import com.hjo2oa.wf.action.engine.domain.ActionCategory;
import com.hjo2oa.wf.action.engine.domain.ActionDefinition;
import com.hjo2oa.wf.action.engine.domain.ActionExecutionRequest;
import com.hjo2oa.wf.action.engine.domain.TaskInstanceSnapshot;
import com.hjo2oa.wf.action.engine.domain.TaskStatus;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceApplicationService;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands;
import com.hjo2oa.wf.process.instance.domain.CandidateType;
import com.hjo2oa.wf.process.instance.domain.TaskInstance;
import com.hjo2oa.wf.process.instance.domain.TaskInstanceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class ProcessInstanceTaskCompletionGateway implements TaskCompletionGateway {

    private final ProcessInstanceApplicationService processInstanceApplicationService;
    private final TaskInstanceRepository taskRepository;

    public ProcessInstanceTaskCompletionGateway(
            ProcessInstanceApplicationService processInstanceApplicationService,
            TaskInstanceRepository taskRepository
    ) {
        this.processInstanceApplicationService = processInstanceApplicationService;
        this.taskRepository = taskRepository;
    }

    @Override
    public TaskStatus apply(
            TaskInstanceSnapshot task,
            ActionDefinition definition,
            ActionExecutionRequest request,
            TaskStatus status
    ) {
        TaskInstance domainTask = taskRepository.findById(task.taskId())
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Process task not found"));
        return switch (definition.category()) {
            case APPROVE, REJECT, RETURN, CUSTOM -> completeTask(domainTask, definition, request);
            case TRANSFER, DELEGATE -> transferTask(domainTask, request);
            case ADD_SIGN -> addSign(domainTask, request);
            case WITHDRAW, TERMINATE -> terminateInstance(domainTask, request);
            case SUSPEND -> suspendInstance(domainTask, request);
            case REDUCE_SIGN -> TaskStatus.REDUCE_SIGNED;
        };
    }

    private TaskStatus completeTask(
            TaskInstance task,
            ActionDefinition definition,
            ActionExecutionRequest request
    ) {
        processInstanceApplicationService.completeTask(new ProcessInstanceCommands.CompleteTaskCommand(
                task.id(),
                definition.code(),
                definition.name(),
                operatorPersonId(task, request),
                operatorOrgId(task, request),
                operatorPositionId(task, request),
                request.opinion(),
                request.targetNodeId(),
                null,
                null,
                List.of(),
                null,
                null,
                null,
                List.of(),
                request.formDataPatch(),
                false,
                request.idempotencyKey(),
                request.idempotencyKey()
        ));
        return definition.category() == ActionCategory.REJECT ? TaskStatus.REJECTED : TaskStatus.COMPLETED;
    }

    private TaskStatus transferTask(TaskInstance task, ActionExecutionRequest request) {
        UUID targetAssigneeId = firstTargetAssignee(request);
        processInstanceApplicationService.transfer(new ProcessInstanceCommands.TransferTaskCommand(
                task.id(),
                targetAssigneeId,
                null,
                null,
                null,
                request.idempotencyKey(),
                request.idempotencyKey()
        ));
        return TaskStatus.TRANSFERRED;
    }

    private TaskStatus addSign(TaskInstance task, ActionExecutionRequest request) {
        processInstanceApplicationService.addSign(new ProcessInstanceCommands.AddSignCommand(
                task.id(),
                operatorPersonId(task, request),
                operatorOrgId(task, request),
                operatorPositionId(task, request),
                request.targetAssigneeIds().stream()
                        .map(target -> participant(UUID.fromString(target)))
                        .toList(),
                request.opinion(),
                request.idempotencyKey(),
                request.idempotencyKey()
        ));
        return TaskStatus.ADD_SIGNED;
    }

    private TaskStatus terminateInstance(TaskInstance task, ActionExecutionRequest request) {
        processInstanceApplicationService.terminate(new ProcessInstanceCommands.TerminateProcessCommand(
                task.instanceId(),
                operatorPersonId(task, request),
                request.opinion(),
                request.idempotencyKey(),
                request.idempotencyKey()
        ));
        return TaskStatus.TERMINATED;
    }

    private TaskStatus suspendInstance(TaskInstance task, ActionExecutionRequest request) {
        processInstanceApplicationService.suspend(new ProcessInstanceCommands.SuspendProcessCommand(
                task.instanceId(),
                operatorPersonId(task, request),
                request.opinion(),
                request.idempotencyKey(),
                request.idempotencyKey()
        ));
        return TaskStatus.PENDING;
    }

    private ProcessInstanceCommands.TaskParticipantCommand participant(UUID personId) {
        return new ProcessInstanceCommands.TaskParticipantCommand(
                personId,
                null,
                null,
                null,
                CandidateType.PERSON,
                List.of(personId)
        );
    }

    private UUID firstTargetAssignee(ActionExecutionRequest request) {
        if (request.targetAssigneeIds().isEmpty()) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Process action target assignee is required");
        }
        return UUID.fromString(request.targetAssigneeIds().get(0));
    }

    private UUID operatorPersonId(TaskInstance task, ActionExecutionRequest request) {
        UUID operatorId = parseUuid(request.operator().personId());
        if (operatorId != null) {
            return operatorId;
        }
        if (task.assigneeId() != null) {
            return task.assigneeId();
        }
        throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "operatorPersonId is required");
    }

    private UUID operatorOrgId(TaskInstance task, ActionExecutionRequest request) {
        UUID operatorOrgId = parseUuid(request.operator().orgId());
        if (operatorOrgId != null) {
            return operatorOrgId;
        }
        if (task.assigneeOrgId() != null) {
            return task.assigneeOrgId();
        }
        throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "operatorOrgId is required");
    }

    private UUID operatorPositionId(TaskInstance task, ActionExecutionRequest request) {
        UUID operatorPositionId = parseUuid(request.operator().positionId());
        if (operatorPositionId != null) {
            return operatorPositionId;
        }
        if (task.assigneePositionId() != null) {
            return task.assigneePositionId();
        }
        throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "operatorPositionId is required");
    }

    private UUID parseUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }
}
