package com.hjo2oa.wf.process.instance.application;

import com.hjo2oa.infra.audit.application.AuditRecordApplicationService;
import com.hjo2oa.infra.audit.application.AuditRecordCommands;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import com.hjo2oa.wf.process.definition.domain.DefinitionStatus;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinition;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinitionRepository;
import com.hjo2oa.wf.process.definition.domain.model.WorkflowDefinitionJsonParser;
import com.hjo2oa.wf.process.definition.domain.model.WorkflowDefinitionModel;
import com.hjo2oa.wf.process.definition.domain.model.WorkflowNodeDefinition;
import com.hjo2oa.wf.process.definition.domain.model.WorkflowParticipantRule;
import com.hjo2oa.wf.process.definition.domain.model.WorkflowRouteCondition;
import com.hjo2oa.wf.process.definition.domain.model.WorkflowRouteDefinition;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.AddSignCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.ClaimTaskCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.CompleteTaskCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.ResumeProcessCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.RouteConditionCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.StartProcessCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.SuspendProcessCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.TaskParticipantCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.TerminateProcessCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.TransferTaskCommand;
import com.hjo2oa.wf.process.instance.domain.CandidateType;
import com.hjo2oa.wf.process.instance.domain.MultiInstanceType;
import com.hjo2oa.wf.process.instance.domain.ProcessEvents;
import com.hjo2oa.wf.process.instance.domain.ProcessHistoryRepository;
import com.hjo2oa.wf.process.instance.domain.ProcessInstance;
import com.hjo2oa.wf.process.instance.domain.ProcessInstanceRepository;
import com.hjo2oa.wf.process.instance.domain.ProcessInstanceViews;
import com.hjo2oa.wf.process.instance.domain.TaskAction;
import com.hjo2oa.wf.process.instance.domain.TaskActionRepository;
import com.hjo2oa.wf.process.instance.domain.TaskInstance;
import com.hjo2oa.wf.process.instance.domain.TaskInstanceRepository;
import com.hjo2oa.wf.process.instance.domain.TaskInstanceStatus;
import com.hjo2oa.wf.process.instance.domain.TaskNodeType;
import com.hjo2oa.wf.process.instance.infrastructure.InMemoryProcessHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessInstanceApplicationService {

    private static final String ANY_COMPLETION = "ANY";
    private static final String ACTION_APPROVE = "approve";
    private static final String ACTION_REJECT = "reject";
    private static final String ACTION_RETURN = "return";

    private final ProcessInstanceRepository instanceRepository;
    private final TaskInstanceRepository taskRepository;
    private final TaskActionRepository actionRepository;
    private final DomainEventPublisher eventPublisher;
    private final ProcessInstanceEngineGateway engineGateway;
    private final ProcessDefinitionRepository definitionRepository;
    private final WorkflowDefinitionJsonParser modelParser;
    private final ParticipantResolver participantResolver;
    private final ProcessHistoryRepository historyRepository;
    private final AuditRecordApplicationService auditService;
    private final Clock clock;

    @Autowired
    public ProcessInstanceApplicationService(
            ProcessInstanceRepository instanceRepository,
            TaskInstanceRepository taskRepository,
            TaskActionRepository actionRepository,
            DomainEventPublisher eventPublisher,
            ObjectProvider<ProcessInstanceEngineGateway> engineGateway,
            ProcessDefinitionRepository definitionRepository,
            WorkflowDefinitionJsonParser modelParser,
            ParticipantResolver participantResolver,
            ObjectProvider<ProcessHistoryRepository> historyRepository,
            ObjectProvider<AuditRecordApplicationService> auditService
    ) {
        this(instanceRepository, taskRepository, actionRepository, eventPublisher,
                engineGateway.getIfAvailable(ProcessInstanceEngineGateway::noop), definitionRepository, modelParser,
                participantResolver, historyRepository.getIfAvailable(InMemoryProcessHistoryRepository::new),
                auditService.getIfAvailable(), Clock.systemUTC());
    }

    public ProcessInstanceApplicationService(
            ProcessInstanceRepository instanceRepository,
            TaskInstanceRepository taskRepository,
            TaskActionRepository actionRepository,
            DomainEventPublisher eventPublisher,
            Clock clock
    ) {
        this(instanceRepository, taskRepository, actionRepository, eventPublisher, ProcessInstanceEngineGateway.noop(),
                null, null, (rule, context) -> List.of(), new InMemoryProcessHistoryRepository(), null, clock);
    }

    public ProcessInstanceApplicationService(
            ProcessInstanceRepository instanceRepository,
            TaskInstanceRepository taskRepository,
            TaskActionRepository actionRepository,
            DomainEventPublisher eventPublisher,
            ProcessInstanceEngineGateway engineGateway,
            ProcessDefinitionRepository definitionRepository,
            WorkflowDefinitionJsonParser modelParser,
            ParticipantResolver participantResolver,
            ProcessHistoryRepository historyRepository,
            AuditRecordApplicationService auditService,
            Clock clock
    ) {
        this.instanceRepository = Objects.requireNonNull(instanceRepository, "instanceRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.actionRepository = Objects.requireNonNull(actionRepository, "actionRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.engineGateway = Objects.requireNonNull(engineGateway, "engineGateway must not be null");
        this.definitionRepository = definitionRepository;
        this.modelParser = modelParser;
        this.participantResolver = Objects.requireNonNull(participantResolver, "participantResolver must not be null");
        this.historyRepository = Objects.requireNonNull(historyRepository, "historyRepository must not be null");
        this.auditService = auditService;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public ProcessInstanceViews.InstanceDetailView start(StartProcessCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (command.idempotencyKey() != null) {
            var existing = instanceRepository.findByTenantAndIdempotencyKey(command.tenantId(), command.idempotencyKey());
            if (existing.isPresent()) {
                return detail(existing.orElseThrow().id());
            }
        }
        Instant now = now();
        RuntimeDefinition runtime = resolveRuntimeDefinition(command);
        WorkflowNodeDefinition firstNode = resolveFirstUserTask(runtime, command.variables());
        List<TaskParticipantCommand> participants = resolveParticipants(firstNode.participantRule(), command, runtime, command.variables());

        ProcessInstance instance = ProcessInstance.start(
                runtime.definitionId(),
                runtime.version(),
                runtime.code(),
                command.businessKey(),
                command.title(),
                runtime.category(),
                command.initiatorId(),
                command.initiatorOrgId(),
                command.initiatorDeptId(),
                command.initiatorPositionId(),
                runtime.formMetadataId(),
                command.formDataId(),
                List.of(firstNode.nodeId()),
                command.tenantId(),
                command.idempotencyKey(),
                now
        );
        engineGateway.start(instance, command);
        instanceRepository.save(instance);
        List<TaskInstance> tasks = createTasks(
                instance.id(),
                firstNode.nodeId(),
                firstNode.name(),
                TaskNodeType.USER_TASK,
                participants,
                toMultiInstanceType(firstNode.multiInstanceType(), command.multiInstanceType()),
                firstNode.completionCondition() == null ? command.completionCondition() : firstNode.completionCondition(),
                command.dueTime(),
                command.tenantId(),
                now
        );
        taskRepository.saveAll(tasks);
        tasks.forEach(task -> historyRepository.recordNode(task, "CREATED", null, null, now));
        historyRepository.recordVariables(instance.id(), null, command.variables(), command.initiatorId(), command.tenantId(), now);
        publishStarted(instance, tasks, now);
        audit("START", "PROCESS_INSTANCE", instance.id().toString(), command.initiatorId(), command.tenantId(),
                command.requestId(), "Started process instance " + instance.title());
        return detail(instance.id());
    }

    @Transactional
    public ProcessInstanceViews.TaskInstanceView claim(ClaimTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        TaskInstance task = loadTaskAllowClaimed(command.taskId());
        if (task.assigneeId() != null && task.assigneeId().equals(command.assigneeId())) {
            return task.toView();
        }
        if (!task.candidateIds().isEmpty() && !task.candidateIds().contains(command.assigneeId())) {
            throw new BizException(SharedErrorDescriptors.FORBIDDEN, "Operator is not a task candidate");
        }
        TaskInstance claimed = task.claim(
                command.assigneeId(),
                command.assigneeOrgId(),
                command.assigneeDeptId(),
                command.assigneePositionId(),
                now()
        );
        engineGateway.claim(task, command);
        TaskInstance saved = taskRepository.save(claimed);
        eventPublisher.publish(ProcessEvents.ProcessTaskClaimedEvent.from(saved, now()));
        audit("CLAIM", "PROCESS_TASK", saved.id().toString(), command.assigneeId(), saved.tenantId(),
                command.requestId(), "Claimed process task " + saved.nodeName());
        return saved.toView();
    }

    @Transactional
    public ProcessInstanceViews.TaskInstanceView transfer(TransferTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant now = now();
        TaskInstance task = loadTask(command.taskId());
        UUID fromPersonId = task.assigneeId();
        TaskInstance transferred = task.transfer(
                command.toAssigneeId(),
                command.toAssigneeOrgId(),
                command.toAssigneeDeptId(),
                command.toAssigneePositionId(),
                now
        );
        engineGateway.transfer(task, command);
        TaskInstance saved = taskRepository.save(transferred);
        eventPublisher.publish(ProcessEvents.ProcessTaskTransferredEvent.from(saved, fromPersonId, now));
        audit("TRANSFER", "PROCESS_TASK", saved.id().toString(), fromPersonId, saved.tenantId(),
                command.requestId(), "Transferred process task to " + command.toAssigneeId());
        return saved.toView();
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

        if (isRejectWithoutTarget(command)) {
            recordAction(task, command, now);
            terminateOpenTasks(instance, "REJECT", now);
            ProcessInstance terminated = instanceRepository.save(instance.terminate(command.opinion(), now));
            eventPublisher.publish(ProcessEvents.ProcessInstanceTerminatedEvent.from(terminated, command.opinion(), now));
            audit("REJECT", "PROCESS_INSTANCE", instance.id().toString(), command.operatorId(), instance.tenantId(),
                    command.requestId(), "Rejected process instance " + instance.title());
            return detail(instance.id());
        }

        TaskInstance completedTask = taskRepository.save(task.complete(now));
        engineGateway.complete(instance, task, command, command.formDataPatch());
        recordAction(task, command, now);
        historyRepository.recordNode(completedTask, "COMPLETED", command.actionCode(), command.operatorId(), now);
        historyRepository.recordVariables(instance.id(), task.id(), command.formDataPatch(), command.operatorId(), instance.tenantId(), now);
        eventPublisher.publish(ProcessEvents.ProcessTaskCompletedEvent.from(completedTask, command.actionCode(), now));

        if (!multiInstanceCanAdvance(completedTask)) {
            return detail(instance.id());
        }
        terminateOpenSiblingsWhenOrSignCompletes(completedTask, now);
        advance(instance, completedTask, command, now);
        return detail(instance.id());
    }

    @Transactional
    public ProcessInstanceViews.InstanceDetailView addSign(AddSignCommand command) {
        TaskInstance task = loadTask(command.taskId());
        Instant now = now();
        List<TaskParticipantCommand> participants = nullToEmpty(command.participants());
        if (participants.isEmpty()) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Add sign target assignee is required");
        }
        List<TaskInstance> added = createTasks(
                task.instanceId(),
                task.nodeId(),
                task.nodeName(),
                task.nodeType(),
                participants,
                MultiInstanceType.PARALLEL,
                task.completionCondition(),
                task.dueTime(),
                task.tenantId(),
                now
        );
        taskRepository.saveAll(added);
        added.forEach(newTask -> {
            historyRepository.recordNode(newTask, "CREATED", "add_sign", command.operatorId(), now);
            publishTaskCreated(newTask, now);
        });
        TaskAction action = TaskAction.record(
                task.id(),
                task.instanceId(),
                "add_sign",
                "Add Sign",
                command.operatorId(),
                command.operatorOrgId(),
                command.operatorPositionId(),
                command.opinion(),
                task.nodeId(),
                Map.of(),
                now
        );
        actionRepository.save(action);
        audit("ADD_SIGN", "PROCESS_TASK", task.id().toString(), command.operatorId(), task.tenantId(),
                command.requestId(), "Added sign task for " + task.nodeName());
        return detail(task.instanceId());
    }

    @Transactional
    public ProcessInstanceViews.InstanceDetailView terminate(TerminateProcessCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant now = now();
        ProcessInstance instance = loadInstance(command.instanceId());
        engineGateway.terminate(instance, command);
        terminateOpenTasks(instance, command.reason(), now);
        ProcessInstance terminated = instanceRepository.save(instance.terminate(command.reason(), now));
        eventPublisher.publish(ProcessEvents.ProcessInstanceTerminatedEvent.from(terminated, command.reason(), now));
        audit("TERMINATE", "PROCESS_INSTANCE", instance.id().toString(), command.operatorId(), instance.tenantId(),
                command.requestId(), "Terminated process instance " + instance.title());
        return detail(instance.id());
    }

    @Transactional
    public ProcessInstanceViews.InstanceDetailView suspend(SuspendProcessCommand command) {
        ProcessInstance instance = loadInstance(command.instanceId());
        ProcessInstance suspended = instanceRepository.save(instance.suspend(now()));
        eventPublisher.publish(ProcessEvents.ProcessInstanceSuspendedEvent.from(suspended, command.reason(), now()));
        audit("SUSPEND", "PROCESS_INSTANCE", instance.id().toString(), command.operatorId(), instance.tenantId(),
                command.requestId(), "Suspended process instance " + instance.title());
        return detail(instance.id());
    }

    @Transactional
    public ProcessInstanceViews.InstanceDetailView resume(ResumeProcessCommand command) {
        ProcessInstance instance = loadInstance(command.instanceId());
        ProcessInstance resumed = instanceRepository.save(instance.resume(now()));
        eventPublisher.publish(ProcessEvents.ProcessInstanceResumedEvent.from(resumed, now()));
        audit("RESUME", "PROCESS_INSTANCE", instance.id().toString(), command.operatorId(), instance.tenantId(),
                command.requestId(), "Resumed process instance " + instance.title());
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
                        .toList(),
                historyRepository.findNodeHistory(instanceId),
                historyRepository.findVariableHistory(instanceId)
        );
    }

    @Transactional(readOnly = true)
    public ProcessInstanceViews.InstanceDetailView timeline(UUID instanceId) {
        return detail(instanceId);
    }

    private void advance(ProcessInstance instance, TaskInstance completedTask, CompleteTaskCommand command, Instant now) {
        RouteSelection route = selectRoute(instance, completedTask, command);
        if (command.endProcess() || route.targetNodeId() == null || isEndNode(instance, route.targetNodeId())) {
            ProcessInstance completed = instanceRepository.save(instance.complete(now));
            terminateOpenTasks(instance, "COMPLETED", now);
            eventPublisher.publish(ProcessEvents.ProcessInstanceCompletedEvent.from(completed, now));
            audit("COMPLETE", "PROCESS_INSTANCE", completed.id().toString(), command.operatorId(), completed.tenantId(),
                    command.requestId(), "Completed process instance " + completed.title());
            return;
        }

        RuntimeDefinition runtime = resolveRuntimeDefinition(instance);
        WorkflowNodeDefinition targetNode = runtime.model().findNode(route.targetNodeId())
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Target node not found"));
        List<TaskParticipantCommand> participants = route.participants().isEmpty()
                ? resolveParticipants(targetNode.participantRule(), instance, command.formDataPatch(), runtime)
                : route.participants();
        List<TaskInstance> nextTasks = createTasks(
                instance.id(),
                targetNode.nodeId(),
                targetNode.name(),
                toNodeType(targetNode.type()),
                participants,
                toMultiInstanceType(targetNode.multiInstanceType(), command.nextMultiInstanceType()),
                targetNode.completionCondition() == null ? command.nextCompletionCondition() : targetNode.completionCondition(),
                command.nextDueTime(),
                instance.tenantId(),
                now
        );
        taskRepository.saveAll(nextTasks);
        instanceRepository.save(instance.moveTo(List.of(targetNode.nodeId()), now));
        nextTasks.forEach(task -> {
            historyRepository.recordNode(task, "CREATED", command.actionCode(), command.operatorId(), now);
            publishTaskCreated(task, now);
        });
    }

    private RouteSelection selectRoute(ProcessInstance instance, TaskInstance task, CompleteTaskCommand command) {
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
        RuntimeDefinition runtime = resolveRuntimeDefinition(instance);
        List<WorkflowRouteDefinition> routes = runtime.model().outgoingRoutes(task.nodeId());
        WorkflowRouteDefinition explicit = command.targetNodeId() == null ? null : routes.stream()
                .filter(route -> command.targetNodeId().equals(route.targetNodeId()))
                .findFirst()
                .orElse(null);
        if (explicit != null) {
            return RouteSelection.from(explicit);
        }
        WorkflowRouteDefinition defaultRoute = null;
        for (WorkflowRouteDefinition route : routes) {
            if (route.defaultRoute()) {
                defaultRoute = route;
            }
            if (matches(route.condition(), command.formDataPatch())) {
                return RouteSelection.from(route);
            }
        }
        if (defaultRoute != null) {
            return RouteSelection.from(defaultRoute);
        }
        return routes.stream().findFirst().map(RouteSelection::from)
                .orElse(new RouteSelection(trimToNull(command.targetNodeId()), command.targetNodeName(),
                        command.targetNodeType(), List.of()));
    }

    private boolean matches(RouteConditionCommand route, Map<String, Object> formDataPatch) {
        if (route.field() == null || route.operator() == null) {
            return false;
        }
        return compare(formDataPatch, route.field(), route.operator(), route.expectedValue());
    }

    private boolean matches(WorkflowRouteCondition condition, Map<String, Object> formDataPatch) {
        if (condition == null || condition.field() == null || condition.operator() == null) {
            return false;
        }
        return compare(formDataPatch, condition.field(), condition.operator(), condition.expectedValue());
    }

    private boolean compare(Map<String, Object> formDataPatch, String field, String operator, String expectedValue) {
        Object value = formDataPatch == null ? null : formDataPatch.get(field);
        String actual = value == null ? null : String.valueOf(value);
        return switch (operator.trim().toUpperCase(Locale.ROOT)) {
            case "EQ", "==" -> Objects.equals(actual, expectedValue);
            case "NE", "!=" -> !Objects.equals(actual, expectedValue);
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
                .toList())
                .forEach(task -> {
                    historyRepository.recordNode(task, "TERMINATED", "OR_SIGN_COMPLETED", null, now);
                    eventPublisher.publish(ProcessEvents.ProcessTaskTerminatedEvent.from(task, "OR_SIGN_COMPLETED", now));
                });
    }

    private void terminateOpenTasks(ProcessInstance instance, String reason, Instant now) {
        taskRepository.saveAll(taskRepository.findOpenByInstanceId(instance.id()).stream()
                        .map(task -> task.terminate(now))
                        .toList())
                .forEach(task -> {
                    historyRepository.recordNode(task, "TERMINATED", reason, null, now);
                    eventPublisher.publish(ProcessEvents.ProcessTaskTerminatedEvent.from(task, reason, now));
                });
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
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Process participant resolve empty");
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

    private RuntimeDefinition resolveRuntimeDefinition(StartProcessCommand command) {
        if (definitionRepository == null || modelParser == null) {
            WorkflowNodeDefinition node = new WorkflowNodeDefinition(
                    requireText(command.firstNodeId(), "firstNodeId"),
                    command.firstNodeName(),
                    "USER_TASK",
                    null,
                    List.of(ACTION_APPROVE),
                    null,
                    command.completionCondition(),
                    Map.of()
            );
            return new RuntimeDefinition(command.definitionId(), command.definitionVersion(), command.definitionCode(),
                    command.category(), command.formMetadataId(), new WorkflowDefinitionModel(List.of(node), List.of()));
        }
        ProcessDefinition definition = definitionRepository.findById(command.definitionId())
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Process definition not found"));
        if (definition.status() != DefinitionStatus.PUBLISHED) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Process definition is not published");
        }
        return new RuntimeDefinition(definition.id(), definition.version(), definition.code(), definition.category(),
                definition.formMetadataId(), modelParser.parse(definition.nodes(), definition.routes()));
    }

    private RuntimeDefinition resolveRuntimeDefinition(ProcessInstance instance) {
        if (definitionRepository == null || modelParser == null) {
            return new RuntimeDefinition(instance.definitionId(), instance.definitionVersion(), instance.definitionCode(),
                    instance.category(), instance.formMetadataId(), new WorkflowDefinitionModel(List.of(), List.of()));
        }
        ProcessDefinition definition = definitionRepository.findById(instance.definitionId())
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Process definition not found"));
        return new RuntimeDefinition(definition.id(), definition.version(), definition.code(), definition.category(),
                definition.formMetadataId(), modelParser.parse(definition.nodes(), definition.routes()));
    }

    private WorkflowNodeDefinition resolveFirstUserTask(RuntimeDefinition runtime, Map<String, Object> variables) {
        WorkflowDefinitionModel model = runtime.model();
        WorkflowNodeDefinition start = model.startNode().orElse(null);
        if (start == null) {
            return model.nodes().stream()
                    .filter(WorkflowNodeDefinition::isUserTask)
                    .findFirst()
                    .orElseThrow(() -> new BizException(
                            SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                            "Process definition has no user task"
                    ));
        }
        return followToUserTask(model, start.nodeId(), variables);
    }

    private WorkflowNodeDefinition followToUserTask(WorkflowDefinitionModel model, String nodeId, Map<String, Object> variables) {
        for (WorkflowRouteDefinition route : model.outgoingRoutes(nodeId)) {
            if (!route.defaultRoute() && route.condition() != null && !matches(route.condition(), variables)) {
                continue;
            }
            WorkflowNodeDefinition target = model.findNode(route.targetNodeId()).orElse(null);
            if (target == null) {
                continue;
            }
            if (target.isUserTask()) {
                return target;
            }
            if (!target.isEnd()) {
                return followToUserTask(model, target.nodeId(), variables);
            }
        }
        throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Process definition start route has no user task");
    }

    private List<TaskParticipantCommand> resolveParticipants(
            WorkflowParticipantRule rule,
            StartProcessCommand command,
            RuntimeDefinition runtime,
            Map<String, Object> variables
    ) {
        List<TaskParticipantCommand> explicit = nullToEmpty(command.participants());
        if (!explicit.isEmpty()) {
            return explicit;
        }
        return resolveParticipants(rule, command.initiatorId(), command.initiatorOrgId(), command.initiatorDeptId(),
                command.initiatorPositionId(), command.tenantId(), variables, runtime);
    }

    private List<TaskParticipantCommand> resolveParticipants(
            WorkflowParticipantRule rule,
            ProcessInstance instance,
            Map<String, Object> variables,
            RuntimeDefinition runtime
    ) {
        return resolveParticipants(rule, instance.initiatorId(), instance.initiatorOrgId(), instance.initiatorDeptId(),
                instance.initiatorPositionId(), instance.tenantId(), variables, runtime);
    }

    private List<TaskParticipantCommand> resolveParticipants(
            WorkflowParticipantRule rule,
            UUID initiatorId,
            UUID initiatorOrgId,
            UUID initiatorDeptId,
            UUID initiatorPositionId,
            UUID tenantId,
            Map<String, Object> variables,
            RuntimeDefinition runtime
    ) {
        List<TaskParticipantCommand> participants = participantResolver.resolve(
                rule,
                new ParticipantResolutionContext(
                        tenantId,
                        initiatorId,
                        initiatorOrgId,
                        initiatorDeptId,
                        initiatorPositionId,
                        variables == null ? Map.of() : variables
                )
        );
        if (participants.isEmpty() && rule != null && "INITIATOR".equalsIgnoreCase(rule.type())) {
            return List.of(new TaskParticipantCommand(
                    initiatorId,
                    initiatorOrgId,
                    initiatorDeptId,
                    initiatorPositionId,
                    CandidateType.PERSON,
                    List.of(initiatorId)
            ));
        }
        if (participants.isEmpty()) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "No candidate resolved for process definition " + runtime.code());
        }
        return participants;
    }

    private boolean operatorCanComplete(TaskInstance task, UUID operatorId) {
        if (task.assigneeId() != null) {
            return task.assigneeId().equals(operatorId);
        }
        return task.candidateIds().isEmpty() || task.candidateIds().contains(operatorId);
    }

    private boolean isRejectWithoutTarget(CompleteTaskCommand command) {
        return ACTION_REJECT.equalsIgnoreCase(command.actionCode()) && trimToNull(command.targetNodeId()) == null;
    }

    private boolean isEndNode(ProcessInstance instance, String nodeId) {
        return resolveRuntimeDefinition(instance).model().findNode(nodeId).filter(WorkflowNodeDefinition::isEnd).isPresent()
                || "END".equalsIgnoreCase(nodeId);
    }

    private void recordAction(TaskInstance task, CompleteTaskCommand command, Instant now) {
        actionRepository.save(TaskAction.record(
                task.id(),
                task.instanceId(),
                command.actionCode(),
                defaultText(command.actionName(), command.actionCode()),
                command.operatorId(),
                command.operatorOrgId(),
                command.operatorPositionId(),
                command.opinion(),
                command.targetNodeId(),
                command.formDataPatch(),
                now
        ));
        audit(command.actionCode().toUpperCase(Locale.ROOT), "PROCESS_TASK", task.id().toString(), command.operatorId(),
                task.tenantId(), command.requestId(), "Executed process task action " + command.actionCode());
    }

    private ProcessInstance loadInstance(UUID instanceId) {
        return instanceRepository.findById(instanceId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Process instance not found"));
    }

    private TaskInstance loadTask(UUID taskId) {
        TaskInstance task = loadTaskAllowClaimed(taskId);
        if (task.status() == TaskInstanceStatus.TERMINATED || task.status() == TaskInstanceStatus.COMPLETED) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Task is already closed");
        }
        return task;
    }

    private TaskInstance loadTaskAllowClaimed(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Task instance not found"));
    }

    private void publishStarted(ProcessInstance instance, List<TaskInstance> tasks, Instant now) {
        eventPublisher.publish(ProcessEvents.ProcessInstanceStartedEvent.from(instance, now));
        tasks.forEach(task -> publishTaskCreated(task, now));
    }

    private void publishTaskCreated(TaskInstance task, Instant now) {
        eventPublisher.publish(ProcessEvents.ProcessTaskCreatedEvent.from(task, now));
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

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private TaskNodeType toNodeType(String value) {
        if (value == null || value.isBlank()) {
            return TaskNodeType.USER_TASK;
        }
        return TaskNodeType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private MultiInstanceType toMultiInstanceType(String configuredValue, MultiInstanceType fallback) {
        if (configuredValue == null || configuredValue.isBlank()) {
            return fallback == null ? MultiInstanceType.NONE : fallback;
        }
        return MultiInstanceType.valueOf(configuredValue.trim().toUpperCase(Locale.ROOT));
    }

    private void audit(
            String actionType,
            String objectType,
            String objectId,
            UUID operatorPersonId,
            UUID tenantId,
            String requestId,
            String summary
    ) {
        if (auditService == null) {
            return;
        }
        auditService.recordAudit(new AuditRecordCommands.RecordAuditCommand(
                "process-instance",
                objectType,
                objectId,
                actionType,
                null,
                operatorPersonId,
                tenantId,
                requestId,
                summary,
                List.of()
        ));
    }

    private record RuntimeDefinition(
            UUID definitionId,
            int version,
            String code,
            String category,
            UUID formMetadataId,
            WorkflowDefinitionModel model
    ) {
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

        static RouteSelection from(WorkflowRouteDefinition route) {
            return new RouteSelection(route.targetNodeId(), route.name(), null, List.of());
        }
    }
}
