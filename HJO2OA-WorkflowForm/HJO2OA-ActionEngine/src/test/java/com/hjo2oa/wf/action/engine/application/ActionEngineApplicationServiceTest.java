package com.hjo2oa.wf.action.engine.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.wf.action.engine.domain.ActionExecutionResult;
import com.hjo2oa.wf.action.engine.domain.ProcessTaskActionExecutedEvent;
import com.hjo2oa.wf.action.engine.domain.ProcessTaskCompletedEvent;
import com.hjo2oa.wf.action.engine.domain.ProcessTaskTransferredEvent;
import com.hjo2oa.wf.action.engine.domain.TaskInstanceSnapshot;
import com.hjo2oa.wf.action.engine.domain.TaskStatus;
import com.hjo2oa.wf.action.engine.infrastructure.InMemoryActionDefinitionRepository;
import com.hjo2oa.wf.action.engine.infrastructure.InMemoryTaskActionRepository;
import com.hjo2oa.wf.action.engine.infrastructure.InMemoryTaskInstanceGateway;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActionEngineApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-27T01:00:00Z"), ZoneOffset.UTC);

    @Test
    void approveShouldCompleteTaskRecordActionAndPublishEvents() {
        Fixture fixture = fixture();
        ActionEngineCommands.ExecuteActionCommand command = command(fixture.taskId, "approve", "idem-1", List.of());

        ActionExecutionResult result = fixture.service.approve(command);

        assertEquals(TaskStatus.COMPLETED, result.taskStatus());
        assertEquals("approve", result.taskAction().actionCode());
        assertEquals(TaskStatus.COMPLETED, fixture.taskGateway.findById(fixture.taskId).orElseThrow().status());
        assertEquals(1, fixture.actionRepository.findByTaskId(fixture.taskId).size());
        assertEquals(2, fixture.events.size());
        assertInstanceOf(ProcessTaskCompletedEvent.class, fixture.events.get(0));
        assertInstanceOf(ProcessTaskActionExecutedEvent.class, fixture.events.get(1));
    }

    @Test
    void transferShouldChangeAssigneeAndPublishTransferredEvent() {
        Fixture fixture = fixture();
        ActionEngineCommands.ExecuteActionCommand command =
                command(fixture.taskId, "transfer", "idem-2", List.of("new-user"));

        ActionExecutionResult result = fixture.service.transfer(command);

        assertEquals(TaskStatus.TRANSFERRED, result.taskStatus());
        TaskInstanceSnapshot task = fixture.taskGateway.findById(fixture.taskId).orElseThrow();
        assertEquals("new-user", task.assigneeId());
        assertInstanceOf(ProcessTaskTransferredEvent.class, fixture.events.get(0));
    }

    @Test
    void repeatedIdempotencyKeyShouldReturnExistingActionWithoutPublishingAgain() {
        Fixture fixture = fixture();
        ActionEngineCommands.ExecuteActionCommand command = command(fixture.taskId, "approve", "idem-3", List.of());

        ActionExecutionResult first = fixture.service.approve(command);
        fixture.taskGateway.save(new TaskInstanceSnapshot(fixture.taskId, fixture.instanceId, "user-1", TaskStatus.PENDING, "tenant-1"));
        ActionExecutionResult second = fixture.service.approve(command);

        assertSame(first.taskAction(), second.taskAction());
        assertEquals(2, fixture.events.size());
        assertEquals(1, fixture.actionRepository.findByTaskId(fixture.taskId).size());
    }

    private Fixture fixture() {
        UUID taskId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        InMemoryTaskInstanceGateway taskGateway = new InMemoryTaskInstanceGateway();
        taskGateway.save(new TaskInstanceSnapshot(taskId, instanceId, "user-1", TaskStatus.PENDING, "tenant-1"));
        InMemoryTaskActionRepository actionRepository = new InMemoryTaskActionRepository();
        List<DomainEvent> events = new ArrayList<>();
        ActionEngineApplicationService service = new ActionEngineApplicationService(
                taskGateway,
                new InMemoryActionDefinitionRepository(),
                actionRepository,
                events::add,
                CLOCK
        );
        return new Fixture(taskId, instanceId, taskGateway, actionRepository, events, service);
    }

    private ActionEngineCommands.ExecuteActionCommand command(
            UUID taskId,
            String actionCode,
            String idempotencyKey,
            List<String> assignees
    ) {
        return new ActionEngineCommands.ExecuteActionCommand(
                taskId,
                actionCode,
                "ok",
                null,
                assignees,
                Map.of(),
                "operator-1",
                "person-1",
                "position-1",
                "org-1",
                idempotencyKey
        );
    }

    private record Fixture(
            UUID taskId,
            UUID instanceId,
            InMemoryTaskInstanceGateway taskGateway,
            InMemoryTaskActionRepository actionRepository,
            List<DomainEvent> events,
            ActionEngineApplicationService service
    ) {
    }
}
