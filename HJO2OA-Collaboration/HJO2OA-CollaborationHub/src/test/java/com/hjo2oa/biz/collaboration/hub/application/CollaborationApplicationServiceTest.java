package com.hjo2oa.biz.collaboration.hub.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hjo2oa.biz.collaboration.hub.domain.CollaborationConstants;
import com.hjo2oa.biz.collaboration.hub.domain.CollaborationEvents;
import com.hjo2oa.biz.collaboration.hub.domain.CollaborationViews;
import com.hjo2oa.biz.collaboration.hub.infrastructure.InMemoryCollaborationRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CollaborationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-29T01:00:00Z");

    private InMemoryCollaborationRepository repository;
    private List<DomainEvent> publishedEvents;
    private CollaborationApplicationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCollaborationRepository();
        publishedEvents = new ArrayList<>();
        service = new CollaborationApplicationService(
                repository,
                publishedEvents::add,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldEnforceWorkspaceMemberPermissions() {
        CollaborationViews.WorkspaceView workspace = createWorkspace(
                ctx("tenant-a", "owner-1", "req-1", "idem-ws-1"),
                List.of(new CollaborationCommands.MemberCommand("viewer-1", "VIEWER", List.of()))
        );

        BizException forbidden = assertThrows(BizException.class, () -> service.createTask(
                ctx("tenant-a", "viewer-1", "req-2", "idem-task-viewer"),
                new CollaborationCommands.CreateTaskCommand(
                        workspace.workspaceId(),
                        "Follow up contract",
                        "Confirm legal comments",
                        "owner-1",
                        List.of("viewer-1"),
                        "HIGH",
                        NOW.plusSeconds(3600)
                )
        ));

        assertEquals("BIZ_COLLAB_FORBIDDEN", forbidden.errorCode());
        assertTrue(service.workspace(ctx("tenant-a", "viewer-1", "req-3", null), workspace.workspaceId())
                .members()
                .stream()
                .anyMatch(member -> member.personId().equals("viewer-1")));
    }

    @Test
    void shouldPublishMentionEventWhenDiscussionMentionsMember() {
        CollaborationViews.WorkspaceView workspace = createWorkspace(
                ctx("tenant-a", "owner-1", "req-1", "idem-ws-2"),
                List.of(new CollaborationCommands.MemberCommand("member-1", "MEMBER", List.of()))
        );

        CollaborationViews.DiscussionView discussion = service.createDiscussion(
                ctx("tenant-a", "owner-1", "req-2", "idem-discussion-1"),
                workspace.workspaceId(),
                new CollaborationCommands.CreateDiscussionCommand(
                        "Launch plan",
                        "Please review the launch checklist",
                        List.of("member-1"),
                        List.of(new CollaborationCommands.AttachmentCommand("attach-1", "plan.pdf"))
                )
        );

        assertEquals("Launch plan", discussion.title());
        assertEquals(1, discussion.attachments().size());
        assertEquals(1, publishedEvents.size());
        CollaborationEvents.MentionCreatedEvent event =
                assertInstanceOf(CollaborationEvents.MentionCreatedEvent.class, publishedEvents.get(0));
        assertEquals("member-1", event.recipientId());
        assertEquals("biz.collaboration.mentioned", event.eventType());
    }

    @Test
    void shouldEnforceTaskStatusFlowAndNotifyParticipants() {
        CollaborationViews.WorkspaceView workspace = createWorkspace(
                ctx("tenant-a", "owner-1", "req-1", "idem-ws-3"),
                List.of(new CollaborationCommands.MemberCommand("assignee-1", "MEMBER", List.of()))
        );
        CollaborationViews.TaskView task = service.createTask(
                ctx("tenant-a", "owner-1", "req-2", "idem-task-1"),
                new CollaborationCommands.CreateTaskCommand(
                        workspace.workspaceId(),
                        "Prepare meeting deck",
                        "Draft the collaboration rollout deck",
                        "assignee-1",
                        List.of("owner-1"),
                        "HIGH",
                        NOW.plusSeconds(7200)
                )
        );

        CollaborationViews.TaskView inProgress = service.changeTaskStatus(
                ctx("tenant-a", "assignee-1", "req-3", "idem-task-status-1"),
                task.taskId(),
                new CollaborationCommands.ChangeTaskStatusCommand("IN_PROGRESS", null, "Started")
        );
        CollaborationViews.TaskView done = service.changeTaskStatus(
                ctx("tenant-a", "assignee-1", "req-4", "idem-task-status-2"),
                task.taskId(),
                new CollaborationCommands.ChangeTaskStatusCommand("DONE", null, "Completed")
        );

        assertEquals(CollaborationConstants.STATUS_IN_PROGRESS, inProgress.status());
        assertEquals(CollaborationConstants.STATUS_DONE, done.status());
        assertThrows(BizException.class, () -> service.changeTaskStatus(
                ctx("tenant-a", "assignee-1", "req-5", "idem-task-status-3"),
                task.taskId(),
                new CollaborationCommands.ChangeTaskStatusCommand("TODO", null, "Rollback")
        ));
        assertTrue(publishedEvents.stream()
                .anyMatch(event -> event instanceof CollaborationEvents.TaskAssignedEvent));
        assertTrue(publishedEvents.stream()
                .anyMatch(event -> event instanceof CollaborationEvents.TaskChangedEvent));
    }

    @Test
    void shouldTriggerMeetingReminderOnce() {
        CollaborationViews.WorkspaceView workspace = createWorkspace(
                ctx("tenant-a", "owner-1", "req-1", "idem-ws-4"),
                List.of(new CollaborationCommands.MemberCommand("attendee-1", "MEMBER", List.of()))
        );
        CollaborationViews.MeetingView meeting = service.createMeeting(
                ctx("tenant-a", "owner-1", "req-2", "idem-meeting-1"),
                new CollaborationCommands.CreateMeetingCommand(
                        workspace.workspaceId(),
                        "Weekly sync",
                        "Review blockers",
                        NOW.plusSeconds(600),
                        NOW.plusSeconds(2400),
                        "Room A",
                        15,
                        List.of("attendee-1")
                )
        );

        CollaborationViews.ReminderTriggerResult first = service.triggerDueMeetingReminders(
                ctx("tenant-a", "owner-1", "req-3", "idem-reminder-1"),
                NOW
        );
        CollaborationViews.ReminderTriggerResult second = service.triggerDueMeetingReminders(
                ctx("tenant-a", "owner-1", "req-4", "idem-reminder-2"),
                NOW
        );

        assertEquals("Weekly sync", meeting.title());
        assertEquals(2, first.sentCount());
        assertEquals(0, second.sentCount());
        assertEquals(2, publishedEvents.stream()
                .filter(event -> event instanceof CollaborationEvents.MeetingReminderDueEvent)
                .count());
    }

    @Test
    void shouldIsolateDataAcrossTenants() {
        CollaborationViews.WorkspaceView workspace = createWorkspace(
                ctx("tenant-a", "owner-1", "req-1", "idem-ws-5"),
                List.of()
        );

        assertTrue(service.listWorkspaces(ctx("tenant-b", "owner-1", "req-2", null)).isEmpty());
        assertThrows(BizException.class, () ->
                service.workspace(ctx("tenant-b", "owner-1", "req-3", null), workspace.workspaceId()));
    }

    private CollaborationViews.WorkspaceView createWorkspace(
            CollaborationRequestContext context,
            List<CollaborationCommands.MemberCommand> members
    ) {
        return service.createWorkspace(
                context,
                new CollaborationCommands.CreateWorkspaceCommand(
                        "space-" + context.idempotencyKey(),
                        "Project Space",
                        "Cross-functional collaboration",
                        "PRIVATE",
                        members
                )
        );
    }

    private static CollaborationRequestContext ctx(
            String tenantId,
            String actorId,
            String requestId,
            String idempotencyKey
    ) {
        return new CollaborationRequestContext(tenantId, actorId, requestId, idempotencyKey, "zh-CN", "Asia/Shanghai");
    }
}
