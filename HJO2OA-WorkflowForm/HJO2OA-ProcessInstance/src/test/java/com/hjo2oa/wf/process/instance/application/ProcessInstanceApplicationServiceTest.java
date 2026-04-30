package com.hjo2oa.wf.process.instance.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinition;
import com.hjo2oa.wf.process.definition.domain.model.WorkflowDefinitionJsonParser;
import com.hjo2oa.wf.process.definition.infrastructure.InMemoryProcessDefinitionRepository;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.CompleteTaskCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.StartProcessCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.TaskParticipantCommand;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands.TerminateProcessCommand;
import com.hjo2oa.wf.process.instance.domain.CandidateType;
import com.hjo2oa.wf.process.instance.domain.MultiInstanceType;
import com.hjo2oa.wf.process.instance.domain.ProcessEvents;
import com.hjo2oa.wf.process.instance.domain.ProcessHistoryRepository;
import com.hjo2oa.wf.process.instance.domain.ProcessInstance;
import com.hjo2oa.wf.process.instance.domain.ProcessInstanceRepository;
import com.hjo2oa.wf.process.instance.domain.ProcessInstanceStatus;
import com.hjo2oa.wf.process.instance.domain.ProcessInstanceViews;
import com.hjo2oa.wf.process.instance.domain.TaskAction;
import com.hjo2oa.wf.process.instance.domain.TaskActionRepository;
import com.hjo2oa.wf.process.instance.domain.TaskInstance;
import com.hjo2oa.wf.process.instance.domain.TaskInstanceRepository;
import com.hjo2oa.wf.process.instance.domain.TaskInstanceStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class ProcessInstanceApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-29T01:00:00Z"), ZoneOffset.UTC);
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DEFINITION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID FORM_METADATA_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID FORM_DATA_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID INITIATOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ORG_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID DEPT_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID POSITION_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID MANAGER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID REVIEWER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void shouldStartApproveCompleteAndWriteHistory() {
        Fixture fixture = fixture(singleApproverNodes());

        ProcessInstanceViews.InstanceDetailView started = fixture.service.start(startCommand("idem-start-approve"));
        ProcessInstanceViews.TaskInstanceView task = started.tasks().get(0);
        ProcessInstanceViews.InstanceDetailView completed =
                fixture.service.completeTask(approveCommand(task.id(), "idem-approve"));

        assertThat(started.instance().status()).isEqualTo(ProcessInstanceStatus.RUNNING);
        assertThat(task.assigneeId()).isEqualTo(MANAGER_ID);
        assertThat(completed.instance().status()).isEqualTo(ProcessInstanceStatus.COMPLETED);
        assertThat(completed.tasks()).extracting(ProcessInstanceViews.TaskInstanceView::status)
                .containsExactly(TaskInstanceStatus.COMPLETED);
        assertThat(completed.actions()).extracting(ProcessInstanceViews.TaskActionView::actionCode)
                .containsExactly("approve");
        assertThat(completed.nodeHistory()).extracting(ProcessInstanceViews.NodeHistoryView::status)
                .contains("CREATED", "COMPLETED");
        assertThat(completed.variableHistory()).extracting(ProcessInstanceViews.VariableHistoryView::variableName)
                .contains("amount", "reason");
        assertThat(fixture.events).extracting(DomainEvent::eventType)
                .contains(
                        ProcessEvents.INSTANCE_STARTED,
                        ProcessEvents.TASK_CREATED,
                        ProcessEvents.TASK_COMPLETED_EVENT_TYPE,
                        ProcessEvents.INSTANCE_COMPLETED
                );
    }

    @Test
    void shouldRejectWithoutTargetAndTerminateInstance() {
        Fixture fixture = fixture(singleApproverNodes());
        ProcessInstanceViews.TaskInstanceView task = fixture.service.start(startCommand("idem-start-reject")).tasks().get(0);

        ProcessInstanceViews.InstanceDetailView rejected =
                fixture.service.completeTask(rejectCommand(task.id(), "idem-reject"));

        assertThat(rejected.instance().status()).isEqualTo(ProcessInstanceStatus.TERMINATED);
        assertThat(rejected.tasks()).extracting(ProcessInstanceViews.TaskInstanceView::status)
                .containsExactly(TaskInstanceStatus.TERMINATED);
        assertThat(rejected.actions()).extracting(ProcessInstanceViews.TaskActionView::actionCode)
                .containsExactly("reject");
        assertThat(fixture.events).extracting(DomainEvent::eventType)
                .contains(ProcessEvents.INSTANCE_TERMINATED, ProcessEvents.TASK_TERMINATED);
    }

    @Test
    void shouldTerminateOpenTasks() {
        Fixture fixture = fixture(singleApproverNodes());
        UUID instanceId = fixture.service.start(startCommand("idem-start-terminate")).instance().id();

        ProcessInstanceViews.InstanceDetailView terminated = fixture.service.terminate(
                new TerminateProcessCommand(instanceId, INITIATOR_ID, "admin stop", "idem-terminate", "req-terminate")
        );

        assertThat(terminated.instance().status()).isEqualTo(ProcessInstanceStatus.TERMINATED);
        assertThat(terminated.tasks()).extracting(ProcessInstanceViews.TaskInstanceView::status)
                .containsExactly(TaskInstanceStatus.TERMINATED);
        assertThat(fixture.events).extracting(DomainEvent::eventType)
                .contains(ProcessEvents.INSTANCE_TERMINATED, ProcessEvents.TASK_TERMINATED);
    }

    @Test
    void shouldResolveMultipleSpecificPeopleAsTaskCandidates() {
        Fixture fixture = fixture(multipleCandidateNodes());

        ProcessInstanceViews.TaskInstanceView task =
                fixture.service.start(startCommand("idem-start-candidates")).tasks().get(0);

        assertThat(task.status()).isEqualTo(TaskInstanceStatus.CREATED);
        assertThat(task.assigneeId()).isNull();
        assertThat(task.candidateType()).isEqualTo(CandidateType.PERSON);
        assertThat(task.candidateIds()).containsExactly(MANAGER_ID, REVIEWER_ID);
    }

    private Fixture fixture(String nodes) {
        InMemoryProcessDefinitionRepository definitionRepository = new InMemoryProcessDefinitionRepository();
        definitionRepository.save(ProcessDefinition.create(
                DEFINITION_ID,
                "expense",
                "Expense Approval",
                "FINANCE",
                1,
                FORM_METADATA_ID,
                "start",
                "end",
                nodes,
                routes(),
                TENANT_ID,
                CLOCK.instant()
        ).publish(INITIATOR_ID, CLOCK.instant()));
        InMemoryProcessInstanceRepository instanceRepository = new InMemoryProcessInstanceRepository();
        InMemoryTaskInstanceRepository taskRepository = new InMemoryTaskInstanceRepository();
        InMemoryTaskActionRepository actionRepository = new InMemoryTaskActionRepository();
        InMemoryProcessHistoryRepository historyRepository = new InMemoryProcessHistoryRepository();
        List<DomainEvent> events = new ArrayList<>();
        ProcessInstanceApplicationService service = new ProcessInstanceApplicationService(
                instanceRepository,
                taskRepository,
                actionRepository,
                events::add,
                ProcessInstanceEngineGateway.noop(),
                definitionRepository,
                new WorkflowDefinitionJsonParser(new ObjectMapper()),
                participantResolver(),
                historyRepository,
                null,
                CLOCK
        );
        return new Fixture(service, events);
    }

    private ParticipantResolver participantResolver() {
        return (rule, context) -> {
            if (rule == null) {
                return List.of();
            }
            if ("INITIATOR".equalsIgnoreCase(rule.type())) {
                return List.of(new TaskParticipantCommand(
                        context.initiatorId(),
                        context.initiatorOrgId(),
                        context.initiatorDeptId(),
                        context.initiatorPositionId(),
                        CandidateType.PERSON,
                        List.of(context.initiatorId())
                ));
            }
            List<UUID> people = rule.ids().stream().map(UUID::fromString).toList();
            if (people.size() == 1) {
                return List.of(new TaskParticipantCommand(
                        people.get(0),
                        ORG_ID,
                        DEPT_ID,
                        POSITION_ID,
                        CandidateType.PERSON,
                        List.of(people.get(0))
                ));
            }
            return List.of(new TaskParticipantCommand(
                    null,
                    null,
                    null,
                    null,
                    CandidateType.PERSON,
                    people
            ));
        };
    }

    private StartProcessCommand startCommand(String idempotencyKey) {
        return new StartProcessCommand(
                DEFINITION_ID,
                0,
                null,
                "EXP-20260429-001",
                "Expense Approval",
                null,
                INITIATOR_ID,
                ORG_ID,
                DEPT_ID,
                POSITION_ID,
                null,
                FORM_DATA_ID,
                null,
                null,
                null,
                Map.of("amount", 1280, "reason", "travel"),
                List.of(),
                MultiInstanceType.NONE,
                null,
                null,
                TENANT_ID,
                idempotencyKey,
                "req-" + idempotencyKey
        );
    }

    private CompleteTaskCommand approveCommand(UUID taskId, String idempotencyKey) {
        return new CompleteTaskCommand(
                taskId,
                "approve",
                "Approve",
                MANAGER_ID,
                ORG_ID,
                POSITION_ID,
                "approved",
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                List.of(),
                Map.of("amount", 1280, "approved", true),
                false,
                idempotencyKey,
                "req-" + idempotencyKey
        );
    }

    private CompleteTaskCommand rejectCommand(UUID taskId, String idempotencyKey) {
        return new CompleteTaskCommand(
                taskId,
                "reject",
                "Reject",
                MANAGER_ID,
                ORG_ID,
                POSITION_ID,
                "not valid",
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                List.of(),
                Map.of(),
                false,
                idempotencyKey,
                "req-" + idempotencyKey
        );
    }

    private String singleApproverNodes() {
        return """
                [
                  {"nodeId":"start","type":"START","name":"Start"},
                  {"nodeId":"manager","type":"USER_TASK","name":"Manager Approval",
                   "participantRule":{"type":"SPECIFIC_PERSON","ids":["%s"]},
                   "actionCodes":["approve","reject","transfer","add_sign"]},
                  {"nodeId":"end","type":"END","name":"End"}
                ]
                """.formatted(MANAGER_ID);
    }

    private String multipleCandidateNodes() {
        return """
                [
                  {"nodeId":"start","type":"START","name":"Start"},
                  {"nodeId":"manager","type":"USER_TASK","name":"Manager Approval",
                   "participantRule":{"type":"SPECIFIC_PERSON","ids":["%s","%s"]},
                   "actionCodes":["approve","reject"]},
                  {"nodeId":"end","type":"END","name":"End"}
                ]
                """.formatted(MANAGER_ID, REVIEWER_ID);
    }

    private String routes() {
        return """
                [
                  {"routeId":"r1","sourceNodeId":"start","targetNodeId":"manager"},
                  {"routeId":"r2","sourceNodeId":"manager","targetNodeId":"end"}
                ]
                """;
    }

    private record Fixture(
            ProcessInstanceApplicationService service,
            List<DomainEvent> events
    ) {
    }

    private static final class InMemoryProcessInstanceRepository implements ProcessInstanceRepository {

        private final Map<UUID, ProcessInstance> storage = new ConcurrentHashMap<>();

        @Override
        public Optional<ProcessInstance> findById(UUID instanceId) {
            return Optional.ofNullable(storage.get(instanceId));
        }

        @Override
        public Optional<ProcessInstance> findByTenantAndIdempotencyKey(UUID tenantId, String idempotencyKey) {
            return storage.values().stream()
                    .filter(instance -> tenantId.equals(instance.tenantId()))
                    .filter(instance -> idempotencyKey.equals(instance.idempotencyKey()))
                    .findFirst();
        }

        @Override
        public List<ProcessInstance> findByInitiator(UUID tenantId, UUID initiatorId) {
            return storage.values().stream()
                    .filter(instance -> tenantId.equals(instance.tenantId()))
                    .filter(instance -> initiatorId.equals(instance.initiatorId()))
                    .sorted(Comparator.comparing(ProcessInstance::createdAt))
                    .toList();
        }

        @Override
        public ProcessInstance save(ProcessInstance instance) {
            storage.put(instance.id(), instance);
            return instance;
        }
    }

    private static final class InMemoryTaskInstanceRepository implements TaskInstanceRepository {

        private final Map<UUID, TaskInstance> storage = new ConcurrentHashMap<>();

        @Override
        public Optional<TaskInstance> findById(UUID taskId) {
            return Optional.ofNullable(storage.get(taskId));
        }

        @Override
        public List<TaskInstance> findByInstanceId(UUID instanceId) {
            return storage.values().stream()
                    .filter(task -> instanceId.equals(task.instanceId()))
                    .sorted(Comparator.comparing(TaskInstance::createdAt))
                    .toList();
        }

        @Override
        public List<TaskInstance> findOpenByInstanceId(UUID instanceId) {
            return findByInstanceId(instanceId).stream().filter(TaskInstance::isOpen).toList();
        }

        @Override
        public List<TaskInstance> findOpenByNode(UUID instanceId, String nodeId) {
            return findByInstanceId(instanceId).stream()
                    .filter(TaskInstance::isOpen)
                    .filter(task -> nodeId.equals(task.nodeId()))
                    .toList();
        }

        @Override
        public TaskInstance save(TaskInstance task) {
            storage.put(task.id(), task);
            return task;
        }

        @Override
        public List<TaskInstance> saveAll(List<TaskInstance> tasks) {
            tasks.forEach(this::save);
            return tasks;
        }
    }

    private static final class InMemoryTaskActionRepository implements TaskActionRepository {

        private final List<TaskAction> actions = new ArrayList<>();

        @Override
        public List<TaskAction> findByInstanceId(UUID instanceId) {
            return actions.stream()
                    .filter(action -> instanceId.equals(action.instanceId()))
                    .sorted(Comparator.comparing(TaskAction::createdAt))
                    .toList();
        }

        @Override
        public List<TaskAction> findByTaskId(UUID taskId) {
            return actions.stream()
                    .filter(action -> taskId.equals(action.taskId()))
                    .sorted(Comparator.comparing(TaskAction::createdAt))
                    .toList();
        }

        @Override
        public TaskAction save(TaskAction action) {
            actions.add(action);
            return action;
        }
    }

    private static final class InMemoryProcessHistoryRepository implements ProcessHistoryRepository {

        private final List<ProcessInstanceViews.NodeHistoryView> nodeHistory = new ArrayList<>();
        private final List<ProcessInstanceViews.VariableHistoryView> variableHistory = new ArrayList<>();

        @Override
        public void recordNode(
                TaskInstance task,
                String historyStatus,
                String actionCode,
                UUID operatorId,
                Instant occurredAt
        ) {
            nodeHistory.add(new ProcessInstanceViews.NodeHistoryView(
                    UUID.randomUUID(),
                    task.instanceId(),
                    task.id(),
                    task.nodeId(),
                    task.nodeName(),
                    task.nodeType(),
                    historyStatus,
                    actionCode,
                    operatorId,
                    occurredAt,
                    task.tenantId()
            ));
        }

        @Override
        public void recordVariables(
                UUID instanceId,
                UUID taskId,
                Map<String, Object> variables,
                UUID operatorId,
                UUID tenantId,
                Instant occurredAt
        ) {
            if (variables == null) {
                return;
            }
            variables.forEach((name, value) -> variableHistory.add(new ProcessInstanceViews.VariableHistoryView(
                    UUID.randomUUID(),
                    instanceId,
                    taskId,
                    name,
                    null,
                    String.valueOf(value),
                    operatorId,
                    occurredAt,
                    tenantId
            )));
        }

        @Override
        public List<ProcessInstanceViews.NodeHistoryView> findNodeHistory(UUID instanceId) {
            return nodeHistory.stream()
                    .filter(history -> instanceId.equals(history.instanceId()))
                    .toList();
        }

        @Override
        public List<ProcessInstanceViews.VariableHistoryView> findVariableHistory(UUID instanceId) {
            return variableHistory.stream()
                    .filter(history -> instanceId.equals(history.instanceId()))
                    .toList();
        }
    }
}
