package com.hjo2oa.wf.process.instance.application;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.ClaimTaskCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.CompleteTaskCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.RouteConditionCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.StartProcessCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.TaskParticipantCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.TerminateProcessCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.TransferTaskCommand;
import com.hjo2oa.wf.process.instance.domain.CandidateType;
import com.hjo2oa.wf.process.instance.domain.MultiInstanceType;
import com.hjo2oa.wf.process.instance.domain.ProcessEvents;
import com.hjo2oa.wf.process.instance.domain.ProcessInstance;
import com.hjo2oa.wf.process.instance.domain.ProcessInstanceRepository;
import com.hjo2oa.wf.process.instance.domain.ProcessInstanceViews;
import com.hjo2oa.wf.process.instance.domain.TaskAction;
import com.hjo2oa.wf.process.instance.domain.TaskActionRepository;
import com.hjo2oa.wf.process.instance.domain.TaskInstance;
import com.hjo2oa.wf.process.instance.domain.TaskInstanceRepository;
import com.hjo2oa.wf.process.instance.domain.TaskInstanceStatus;
import com.hjo2oa.wf.process.instance.domain.TaskNodeType;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessInstanceApplicationService {

    private static final String ANY_COMPLETION = "ANY";

    private final ProcessInstanceRepository instanceRepository;
    private final TaskInstanceRepository taskRepository;
    private final TaskActionRepository actionRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;
    @Autowired
    public ProcessInstanceApplicationService(
            ProcessInstanceRepository instanceRepository,
            TaskInstanceRepository taskRepository,
            TaskActionRepository actionRepository,
            DomainEventPublisher eventPublisher
    ) {
        this(instanceRepository, taskRepository, actionRepository, eventPublisher, Clock.systemUTC());
    }
    public ProcessInstanceApplicationService(
            ProcessInstanceRepository instanceRepository,
            TaskInstanceRepository taskRepository,
            TaskActionRepository actionRepository,
            DomainEventPublisher eventPublisher,
            Clock clock
    ) {
        this.instanceRepository = Objects.requireNonNull(instanceRepository, "instanceRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.actionRepository = Objects.requireNonNull(actionRepository, "actionRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public ProcessInstanceViews.InstanceDetailView start(StartProcessCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant now = now();
        String firstNodeId = requireText(command.firstNodeId(), "firstNodeId");
        ProcessInstance instance = ProcessInstance.start(
                command.definitionId(),
                command.definitionVersion(),
                command.definitionCode(),
                command.title(),
                command.category(),
                command.initiatorId(),
                command.initiatorOrgId(),
                command.initiatorDeptId(),
                command.initiatorPositionId(),
                command.formMetadataId(),
                command.formDataId(),
                List.of(firstNodeId),
                command.tenantId(),
                now
        );
        instanceRepository.save(instance);
        List<TaskInstance> tasks = createTasks(
                instance.id(),
                firstNodeId,
                command.firstNodeName(),
                command.firstNodeType(),
                command.participants(),
                command.multiInstanceType(),
                command.completionCondition(),
                command.dueTime(),
                command.tenantId(),
                now
        );
        taskRepository.saveAll(tasks);
        publishStarted(instance, now);
        tasks.forEach(task -> publishTaskCreated(task, now));
        return detail(instance.id());
    }

    @Transactional
    public ProcessInstanceViews.TaskInstanceView claim(ClaimTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        TaskInstance task = loadTask(command.taskId());
        TaskInstance claimed = task.claim(
                command.assigneeId(),
                command.assigneeOrgId(),
                command.assigneeDeptId(),
                command.assigneePositionId(),
                now()
        );
        return taskRepository.save(claimed).toView();
    }

    @Transactional
    public ProcessInstanceViews.TaskInstanceView transfer(TransferTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        TaskInstance task = loadTask(command.taskId());
        TaskInstance transferred = task.transfer(
                command.toAssigneeId(),
                command.toAssigneeOrgId(),
                command.toAssigneeDeptId(),
                command.toAssigneePositionId(),
                now()
        );
        return taskRepository.save(transferred).toView();
    }

    @Transactional
    public ProcessInstanceViews.InstanceDetailView completeTask(CompleteTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant now = now();
        TaskInstance task = loadTask(command.taskId());
        ProcessInstance instance = loadInstance(task.instanceId());
        if (!operatorCanComplete(task, command.operatorId())) {
            throw new BizException(SharedErrorDescriptors.FORBIDDEN, "Operator cannot complete this task");
        }

        TaskInstance completedTask = taskRepository.save(task.complete(now));
        TaskAction action = TaskAction.record(
                task.id(),
                task.instanceId(),
                command.actionCode(),
                command.actionName(),
                command.operatorId(),
                command.operatorOrgId(),
                command.operatorPositionId(),
                command.opinion(),
                command.targetNodeId(),
                command.formDataPatch(),
                now
        );
        actionRepository.save(action);
        eventPublisher.publish(new ProcessEvents.ProcessTaskCompletedEvent(
                UUID.randomUUID(),
                ProcessEvents.TASK_COMPLETED_EVENT_TYPE,
                now,
                instance.tenantId().toString(),
                completedTask.id(),
                instance.id(),
                command.actionCode()
        ));

        if (!multiInstanceCanAdvance(completedTask)) {
            return detail(instance.id());
        }
        terminateOpenSiblingsWhenOrSignCompletes(completedTask, now);
        advance(instance, command, now);
        return detail(instance.id());
    }

    @Transactional
    public ProcessInstanceViews.InstanceDetailView terminate(TerminateProcessCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant now = now();
        ProcessInstance instance = loadInstance(command.instanceId());
        taskRepository.saveAll(taskRepository.findOpenByInstanceId(instance.id()).stream()
                .map(task -> task.terminate(now))
                .toList());
        instanceRepository.save(instance.terminate(command.reason(), now));
        return detail(instance.id());
    }

    @Transactional(readOnly = true)
    public ProcessInstanceViews.InstanceDetailView detail(UUID instanceId) {
        ProcessInstance instance = loadInstance(instanceId);
        return new ProcessInstanceViews.InstanceDetailView(
                instance.toView(),
                taskRepository.findByInstanceId(instanceId).stream()
                        .sorted(Comparator.comparing(TaskInstance::createdAt))
                        .map(TaskInstance::toView)
                        .toList(),
                actionRepository.findByInstanceId(instanceId).stream()
                        .sorted(Comparator.comparing(TaskAction::createdAt))
                        .map(TaskAction::toView)
                        .toList()
        );
    }

    private void advance(ProcessInstance instance, CompleteTaskCommand command, Instant now) {
        RouteSelection route = selectRoute(command);
        if (command.endProcess() || route.targetNodeId() == null || "END".equalsIgnoreCase(route.targetNodeId())) {
            ProcessInstance completed = instanceRepository.save(instance.complete(now));
            taskRepository.saveAll(taskRepository.findOpenByInstanceId(instance.id()).stream()
                    .map(task -> task.terminate(now))
                    .toList());
            eventPublisher.publish(new ProcessEvents.ProcessInstanceCompletedEvent(
                    UUID.randomUUID(),
                    "process.instance.completed",
                    now,
                    completed.tenantId().toString(),
                    completed.id(),
                    completed.endTime()
            ));
            return;
        }

        List<TaskParticipantCommand> participants = route.participants().isEmpty()
                ? nullToEmpty(command.nextParticipants())
                : route.participants();
        List<TaskInstance> nextTasks = createTasks(
                instance.id(),
                route.targetNodeId(),
                route.targetNodeName(),
                route.targetNodeType(),
                participants,
                command.nextMultiInstanceType(),
                command.nextCompletionCondition(),
                command.nextDueTime(),
                instance.tenantId(),
                now
        );
        taskRepository.saveAll(nextTasks);
        instanceRepository.save(instance.moveTo(List.of(route.targetNodeId()), now));
        nextTasks.forEach(task -> publishTaskCreated(task, now));
    }

    private RouteSelection selectRoute(CompleteTaskCommand command) {
        if (command.routeConditions() != null && !command.routeConditions().isEmpty()) {
            RouteConditionCommand defaultRoute = null;
            for (RouteConditionCommand route : command.routeConditions()) {
                if (route.defaultRoute()) {
                    defaultRoute = route;
                }
                if (matches(route, command.formDataPatch())) {
                    return RouteSelection.from(route);
                }
            }
            if (defaultRoute != null) {
                return RouteSelection.from(defaultRoute);
            }
        }
        return new RouteSelection(
                trimToNull(command.targetNodeId()),
                command.targetNodeName(),
                command.targetNodeType(),
                List.of()
        );
    }

    private boolean matches(RouteConditionCommand route, Map<String, Object> formDataPatch) {
        if (route.field() == null || route.operator() == null) {
            return false;
        }
        Object value = formDataPatch == null ? null : formDataPatch.get(route.field());
        String actual = value == null ? null : String.valueOf(value);
        String expected = route.expectedValue();
        return switch (route.operator().trim().toUpperCase(java.util.Locale.ROOT)) {
            case "EQ", "==" -> Objects.equals(actual, expected);
            case "NE", "!=" -> !Objects.equals(actual, expected);
            case "PRESENT" -> actual != null && !actual.isBlank();
            default -> false;
        };
    }

    private boolean multiInstanceCanAdvance(TaskInstance completedTask) {
        if (completedTask.multiInstanceType() == MultiInstanceType.NONE) {
            return true;
        }
        List<TaskInstance> siblings = taskRepository.findOpenByNode(completedTask.instanceId(), completedTask.nodeId());
        if (ANY_COMPLETION.equalsIgnoreCase(completedTask.completionCondition())) {
            return true;
        }
        return siblings.isEmpty();
    }

    private void terminateOpenSiblingsWhenOrSignCompletes(TaskInstance completedTask, Instant now) {
        if (!ANY_COMPLETION.equalsIgnoreCase(completedTask.completionCondition())) {
            return;
        }
        taskRepository.saveAll(taskRepository.findOpenByNode(completedTask.instanceId(), completedTask.nodeId()).stream()
                .filter(task -> !task.id().equals(completedTask.id()))
                .map(task -> task.terminate(now))
                .toList());
    }

    private List<TaskInstance> createTasks(
            UUID instanceId,
            String nodeId,
            String nodeName,
            TaskNodeType nodeType,
            List<TaskParticipantCommand> participants,
            MultiInstanceType multiInstanceType,
            String completionCondition,
            Instant dueTime,
            UUID tenantId,
            Instant now
    ) {
        String resolvedName = nodeName == null || nodeName.isBlank() ? nodeId : nodeName;
        TaskNodeType resolvedType = nodeType == null ? TaskNodeType.USER_TASK : nodeType;
        List<TaskParticipantCommand> resolvedParticipants = nullToEmpty(participants);
        if (resolvedParticipants.isEmpty()) {
            resolvedParticipants = List.of(new TaskParticipantCommand(null, null, null, null, CandidateType.PERSON, List.of()));
        }
        List<TaskInstance> tasks = new ArrayList<>(resolvedParticipants.size());
        for (TaskParticipantCommand participant : resolvedParticipants) {
            tasks.add(TaskInstance.create(
                    instanceId,
                    nodeId,
                    resolvedName,
                    resolvedType,
                    participant.assigneeId(),
                    participant.assigneeOrgId(),
                    participant.assigneeDeptId(),
                    participant.assigneePositionId(),
                    participant.candidateType(),
                    participant.candidateIds(),
                    multiInstanceType,
                    completionCondition,
                    dueTime,
                    tenantId,
                    now
            ));
        }
        return tasks;
    }

    private boolean operatorCanComplete(TaskInstance task, UUID operatorId) {
        if (task.assigneeId() != null) {
            return task.assigneeId().equals(operatorId);
        }
        return task.candidateIds().isEmpty() || task.candidateIds().contains(operatorId);
    }

    private ProcessInstance loadInstance(UUID instanceId) {
        return instanceRepository.findById(instanceId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Process instance not found"));
    }

    private TaskInstance loadTask(UUID taskId) {
        TaskInstance task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Task instance not found"));
        if (task.status() == TaskInstanceStatus.TERMINATED || task.status() == TaskInstanceStatus.COMPLETED) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Task is already closed");
        }
        return task;
    }

    private void publishStarted(ProcessInstance instance, Instant now) {
        eventPublisher.publish(new ProcessEvents.ProcessInstanceStartedEvent(
                UUID.randomUUID(),
                "process.instance.started",
                now,
                instance.tenantId().toString(),
                instance.id(),
                instance.definitionId(),
                instance.initiatorId()
        ));
    }

    private void publishTaskCreated(TaskInstance task, Instant now) {
        eventPublisher.publish(new ProcessEvents.ProcessTaskCreatedEvent(
                UUID.randomUUID(),
                "process.task.created",
                now,
                task.tenantId().toString(),
                task.id(),
                task.instanceId(),
                task.nodeId(),
                task.candidateType(),
                task.candidateIds()
        ));
    }

    private Instant now() {
        return clock.instant();
    }

    private static <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record RouteSelection(
            String targetNodeId,
            String targetNodeName,
            TaskNodeType targetNodeType,
            List<TaskParticipantCommand> participants
    ) {

        static RouteSelection from(RouteConditionCommand command) {
            return new RouteSelection(
                    trimToNull(command.targetNodeId()),
                    command.targetNodeName(),
                    command.targetNodeType(),
                    nullToEmpty(command.participants())
            );
        }
    }
}
