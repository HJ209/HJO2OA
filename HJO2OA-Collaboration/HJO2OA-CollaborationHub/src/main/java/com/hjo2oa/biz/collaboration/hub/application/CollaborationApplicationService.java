package com.hjo2oa.biz.collaboration.hub.application;

import com.hjo2oa.biz.collaboration.hub.domain.CollaborationConstants;
import com.hjo2oa.biz.collaboration.hub.domain.CollaborationErrorDescriptors;
import com.hjo2oa.biz.collaboration.hub.domain.CollaborationEvents;
import com.hjo2oa.biz.collaboration.hub.domain.CollaborationRecords;
import com.hjo2oa.biz.collaboration.hub.domain.CollaborationRepository;
import com.hjo2oa.biz.collaboration.hub.domain.CollaborationViews;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CollaborationApplicationService {

    private static final int DEFAULT_REMINDER_MINUTES = 15;
    private static final int AUDIT_LIMIT = 80;

    private final CollaborationRepository repository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    @Autowired
    public CollaborationApplicationService(
            CollaborationRepository repository,
            DomainEventPublisher domainEventPublisher
    ) {
        this(repository, domainEventPublisher, Clock.systemUTC());
    }

    public CollaborationApplicationService(
            CollaborationRepository repository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public CollaborationViews.CollaborationSnapshot snapshot(CollaborationRequestContext context) {
        List<CollaborationViews.WorkspaceView> workspaces = listWorkspaces(context);
        Set<String> visibleWorkspaceIds = workspaces.stream()
                .map(CollaborationViews.WorkspaceView::workspaceId)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        List<CollaborationViews.DiscussionView> discussions = visibleWorkspaceIds.stream()
                .flatMap(workspaceId -> repository.listDiscussions(context.tenantId(), workspaceId).stream())
                .sorted(Comparator.comparing(CollaborationRecords.Discussion::updatedAt).reversed())
                .limit(20)
                .map(discussion -> toDiscussionView(context, discussion))
                .toList();
        List<CollaborationViews.TaskView> tasks = repository.listTasks(context.tenantId()).stream()
                .filter(task -> visibleWorkspaceIds.contains(task.workspaceId()) || taskVisibleTo(task, context.actorId()))
                .sorted(Comparator.comparing(CollaborationRecords.Task::updatedAt).reversed())
                .limit(30)
                .map(task -> toTaskView(context, task))
                .toList();
        List<CollaborationViews.MeetingView> meetings = repository.listMeetings(context.tenantId()).stream()
                .filter(meeting -> visibleWorkspaceIds.contains(meeting.workspaceId())
                        || meetingVisibleTo(meeting, context.actorId()))
                .sorted(Comparator.comparing(CollaborationRecords.Meeting::startAt))
                .limit(30)
                .map(this::toMeetingView)
                .toList();
        return new CollaborationViews.CollaborationSnapshot(
                workspaces,
                discussions,
                tasks,
                meetings,
                listAuditTrail(context)
        );
    }

    public List<CollaborationViews.WorkspaceView> listWorkspaces(CollaborationRequestContext context) {
        return repository.listWorkspaces(context.tenantId()).stream()
                .filter(workspace -> isWorkspaceVisible(workspace, context.actorId()))
                .sorted(Comparator.comparing(CollaborationRecords.Workspace::updatedAt).reversed())
                .map(this::toWorkspaceView)
                .toList();
    }

    public CollaborationViews.WorkspaceView workspace(CollaborationRequestContext context, String workspaceId) {
        CollaborationRecords.Workspace workspace = requireVisibleWorkspace(context, workspaceId);
        audit(context, "WORKSPACE_VIEWED", CollaborationConstants.OBJECT_WORKSPACE, workspaceId, context.language());
        return toWorkspaceView(workspace);
    }

    public CollaborationViews.WorkspaceView createWorkspace(
            CollaborationRequestContext context,
            CollaborationCommands.CreateWorkspaceCommand command
    ) {
        return withIdempotency(
                context,
                "CREATE_WORKSPACE",
                "WORKSPACE",
                id -> workspace(context, id),
                () -> doCreateWorkspace(context, command)
        );
    }

    public CollaborationViews.WorkspaceView replaceMembers(
            CollaborationRequestContext context,
            String workspaceId,
            CollaborationCommands.ReplaceMembersCommand command
    ) {
        CollaborationRecords.Workspace workspace = requireWorkspace(context.tenantId(), workspaceId);
        requireWorkspacePermission(context, workspaceId, CollaborationConstants.PERMISSION_MANAGE_MEMBERS);
        repository.deleteMembersByWorkspace(context.tenantId(), workspaceId);
        List<CollaborationCommands.MemberCommand> members = new ArrayList<>(nullToList(command.members()));
        members.add(new CollaborationCommands.MemberCommand(
                workspace.ownerId(),
                CollaborationConstants.ROLE_OWNER,
                permissionsForRole(CollaborationConstants.ROLE_OWNER)
        ));
        for (CollaborationCommands.MemberCommand member : dedupeMembers(members)) {
            saveMember(context.tenantId(), workspaceId, member, now());
        }
        audit(context, "WORKSPACE_MEMBERS_REPLACED", CollaborationConstants.OBJECT_WORKSPACE, workspaceId, "members");
        return toWorkspaceView(workspace);
    }

    public List<CollaborationViews.DiscussionView> listDiscussions(
            CollaborationRequestContext context,
            String workspaceId
    ) {
        requireVisibleWorkspace(context, workspaceId);
        return repository.listDiscussions(context.tenantId(), workspaceId).stream()
                .sorted(Comparator
                        .comparing(CollaborationRecords.Discussion::pinned).reversed()
                        .thenComparing(CollaborationRecords.Discussion::updatedAt, Comparator.reverseOrder()))
                .map(discussion -> toDiscussionView(context, discussion))
                .toList();
    }

    public CollaborationViews.DiscussionView createDiscussion(
            CollaborationRequestContext context,
            String workspaceId,
            CollaborationCommands.CreateDiscussionCommand command
    ) {
        return withIdempotency(
                context,
                "CREATE_DISCUSSION:" + workspaceId,
                "DISCUSSION",
                id -> discussion(context, id),
                () -> doCreateDiscussion(context, workspaceId, command)
        );
    }

    public CollaborationViews.DiscussionView discussion(CollaborationRequestContext context, String discussionId) {
        CollaborationRecords.Discussion discussion = requireDiscussion(context.tenantId(), discussionId);
        requireVisibleWorkspace(context, discussion.workspaceId());
        return toDiscussionView(context, discussion);
    }

    public CollaborationViews.DiscussionView replyDiscussion(
            CollaborationRequestContext context,
            String discussionId,
            CollaborationCommands.ReplyDiscussionCommand command
    ) {
        return withIdempotency(
                context,
                "REPLY_DISCUSSION:" + discussionId,
                "DISCUSSION",
                id -> discussion(context, id),
                () -> doReplyDiscussion(context, discussionId, command)
        );
    }

    public CollaborationViews.DiscussionView markDiscussionRead(
            CollaborationRequestContext context,
            String discussionId
    ) {
        CollaborationRecords.Discussion discussion = requireDiscussion(context.tenantId(), discussionId);
        requireVisibleWorkspace(context, discussion.workspaceId());
        Instant now = now();
        repository.saveRead(new CollaborationRecords.DiscussionRead(
                id(),
                context.tenantId(),
                discussionId,
                context.actorId(),
                now
        ));
        audit(context, "DISCUSSION_READ", CollaborationConstants.OBJECT_DISCUSSION, discussionId, "readAt=" + now);
        return toDiscussionView(context, discussion);
    }

    public CollaborationViews.DiscussionView pinDiscussion(
            CollaborationRequestContext context,
            String discussionId,
            boolean pinned
    ) {
        CollaborationRecords.Discussion discussion = requireDiscussion(context.tenantId(), discussionId);
        requireWorkspacePermission(context, discussion.workspaceId(), CollaborationConstants.PERMISSION_MANAGE_DISCUSSION);
        CollaborationRecords.Discussion updated = new CollaborationRecords.Discussion(
                discussion.discussionId(),
                discussion.tenantId(),
                discussion.workspaceId(),
                discussion.title(),
                discussion.body(),
                discussion.authorId(),
                discussion.status(),
                pinned,
                discussion.createdAt(),
                now(),
                discussion.closedAt()
        );
        repository.saveDiscussion(updated);
        audit(context, pinned ? "DISCUSSION_PINNED" : "DISCUSSION_UNPINNED",
                CollaborationConstants.OBJECT_DISCUSSION, discussionId, Boolean.toString(pinned));
        return toDiscussionView(context, updated);
    }

    public CollaborationViews.DiscussionView closeDiscussion(
            CollaborationRequestContext context,
            String discussionId
    ) {
        CollaborationRecords.Discussion discussion = requireDiscussion(context.tenantId(), discussionId);
        requireWorkspacePermission(context, discussion.workspaceId(), CollaborationConstants.PERMISSION_MANAGE_DISCUSSION);
        Instant now = now();
        CollaborationRecords.Discussion updated = new CollaborationRecords.Discussion(
                discussion.discussionId(),
                discussion.tenantId(),
                discussion.workspaceId(),
                discussion.title(),
                discussion.body(),
                discussion.authorId(),
                CollaborationConstants.STATUS_CLOSED,
                discussion.pinned(),
                discussion.createdAt(),
                now,
                now
        );
        repository.saveDiscussion(updated);
        audit(context, "DISCUSSION_CLOSED", CollaborationConstants.OBJECT_DISCUSSION, discussionId, "closed");
        return toDiscussionView(context, updated);
    }

    public List<CollaborationViews.CommentView> listComments(
            CollaborationRequestContext context,
            String objectType,
            String objectId
    ) {
        String workspaceId = resolveWorkspaceId(context, objectType, objectId, null);
        requireVisibleWorkspace(context, workspaceId);
        return repository.listComments(context.tenantId(), normalizeCode(objectType), objectId).stream()
                .map(this::toCommentView)
                .toList();
    }

    public CollaborationViews.CommentView createComment(
            CollaborationRequestContext context,
            CollaborationCommands.CreateCommentCommand command
    ) {
        return withIdempotency(
                context,
                "CREATE_COMMENT:" + normalizeCode(command.objectType()) + ":" + command.objectId(),
                "COMMENT",
                id -> comment(context, id),
                () -> doCreateComment(context, command)
        );
    }

    public CollaborationViews.CommentView deleteComment(CollaborationRequestContext context, String commentId) {
        CollaborationRecords.Comment comment = requireComment(context.tenantId(), commentId);
        if (!comment.authorId().equals(context.actorId())) {
            requireWorkspacePermission(context, comment.workspaceId(), CollaborationConstants.PERMISSION_MANAGE_DISCUSSION);
        }
        Instant now = now();
        CollaborationRecords.Comment deleted = new CollaborationRecords.Comment(
                comment.commentId(),
                comment.tenantId(),
                comment.workspaceId(),
                comment.objectType(),
                comment.objectId(),
                comment.authorId(),
                comment.body(),
                true,
                comment.createdAt(),
                now,
                now
        );
        repository.saveComment(deleted);
        audit(context, "COMMENT_DELETED", "COMMENT", commentId, comment.objectType() + ":" + comment.objectId());
        return toCommentView(deleted);
    }

    public CollaborationViews.CommentView comment(CollaborationRequestContext context, String commentId) {
        CollaborationRecords.Comment comment = requireComment(context.tenantId(), commentId);
        requireVisibleWorkspace(context, comment.workspaceId());
        return toCommentView(comment);
    }

    public List<CollaborationViews.TaskView> listTasks(CollaborationRequestContext context, String workspaceId) {
        return repository.listTasks(context.tenantId()).stream()
                .filter(task -> workspaceId == null || workspaceId.isBlank() || task.workspaceId().equals(workspaceId))
                .filter(task -> isWorkspaceVisible(requireWorkspace(context.tenantId(), task.workspaceId()), context.actorId())
                        || taskVisibleTo(task, context.actorId()))
                .sorted(Comparator.comparing(CollaborationRecords.Task::updatedAt).reversed())
                .map(task -> toTaskView(context, task))
                .toList();
    }

    public CollaborationViews.TaskView createTask(
            CollaborationRequestContext context,
            CollaborationCommands.CreateTaskCommand command
    ) {
        return withIdempotency(
                context,
                "CREATE_TASK:" + command.workspaceId(),
                "TASK",
                id -> task(context, id),
                () -> doCreateTask(context, command)
        );
    }

    public CollaborationViews.TaskView task(CollaborationRequestContext context, String taskId) {
        CollaborationRecords.Task task = requireTask(context.tenantId(), taskId);
        if (!isWorkspaceVisible(requireWorkspace(context.tenantId(), task.workspaceId()), context.actorId())
                && !taskVisibleTo(task, context.actorId())) {
            throw new BizException(CollaborationErrorDescriptors.FORBIDDEN, "No permission to view task");
        }
        return toTaskView(context, task);
    }

    public CollaborationViews.TaskView changeTaskStatus(
            CollaborationRequestContext context,
            String taskId,
            CollaborationCommands.ChangeTaskStatusCommand command
    ) {
        return withIdempotency(
                context,
                "CHANGE_TASK_STATUS:" + taskId + ":" + normalizeCode(command.status()),
                "TASK",
                id -> task(context, id),
                () -> doChangeTaskStatus(context, taskId, command)
        );
    }

    public CollaborationViews.CommentView commentOnTask(
            CollaborationRequestContext context,
            String taskId,
            CollaborationCommands.CreateCommentCommand command
    ) {
        CollaborationRecords.Task task = requireTask(context.tenantId(), taskId);
        return createComment(context, new CollaborationCommands.CreateCommentCommand(
                task.workspaceId(),
                CollaborationConstants.OBJECT_TASK,
                taskId,
                command.body(),
                command.mentionPersonIds(),
                command.attachments()
        ));
    }

    public List<CollaborationViews.MeetingView> listMeetings(CollaborationRequestContext context, String workspaceId) {
        return repository.listMeetings(context.tenantId()).stream()
                .filter(meeting -> workspaceId == null || workspaceId.isBlank() || meeting.workspaceId().equals(workspaceId))
                .filter(meeting -> isWorkspaceVisible(requireWorkspace(context.tenantId(), meeting.workspaceId()), context.actorId())
                        || meetingVisibleTo(meeting, context.actorId()))
                .sorted(Comparator.comparing(CollaborationRecords.Meeting::startAt))
                .map(this::toMeetingView)
                .toList();
    }

    public CollaborationViews.MeetingView createMeeting(
            CollaborationRequestContext context,
            CollaborationCommands.CreateMeetingCommand command
    ) {
        return withIdempotency(
                context,
                "CREATE_MEETING:" + command.workspaceId(),
                "MEETING",
                id -> meeting(context, id),
                () -> doCreateMeeting(context, command)
        );
    }

    public CollaborationViews.MeetingView meeting(CollaborationRequestContext context, String meetingId) {
        CollaborationRecords.Meeting meeting = requireMeeting(context.tenantId(), meetingId);
        if (!isWorkspaceVisible(requireWorkspace(context.tenantId(), meeting.workspaceId()), context.actorId())
                && !meetingVisibleTo(meeting, context.actorId())) {
            throw new BizException(CollaborationErrorDescriptors.FORBIDDEN, "No permission to view meeting");
        }
        return toMeetingView(meeting);
    }

    public CollaborationViews.MeetingView changeMeetingStatus(
            CollaborationRequestContext context,
            String meetingId,
            CollaborationCommands.ChangeMeetingStatusCommand command
    ) {
        return withIdempotency(
                context,
                "CHANGE_MEETING_STATUS:" + meetingId + ":" + normalizeCode(command.status()),
                "MEETING",
                id -> meeting(context, id),
                () -> doChangeMeetingStatus(context, meetingId, command)
        );
    }

    public CollaborationViews.MeetingView publishMeetingMinutes(
            CollaborationRequestContext context,
            String meetingId,
            CollaborationCommands.PublishMeetingMinutesCommand command
    ) {
        return withIdempotency(
                context,
                "PUBLISH_MEETING_MINUTES:" + meetingId,
                "MEETING",
                id -> meeting(context, id),
                () -> doPublishMeetingMinutes(context, meetingId, command)
        );
    }

    public CollaborationViews.ReminderTriggerResult triggerDueMeetingReminders(
            CollaborationRequestContext context,
            Instant dueAt
    ) {
        Instant triggerAt = dueAt == null ? now() : dueAt;
        List<CollaborationRecords.MeetingReminder> dueReminders =
                repository.listDueMeetingReminders(context.tenantId(), triggerAt);
        int sentCount = 0;
        for (CollaborationRecords.MeetingReminder reminder : dueReminders) {
            Optional<CollaborationRecords.Meeting> meeting = repository.findMeeting(context.tenantId(), reminder.meetingId());
            if (meeting.isEmpty() || CollaborationConstants.STATUS_CANCELLED.equals(meeting.orElseThrow().status())) {
                continue;
            }
            CollaborationRecords.MeetingReminder sent = new CollaborationRecords.MeetingReminder(
                    reminder.reminderId(),
                    reminder.tenantId(),
                    reminder.meetingId(),
                    reminder.participantId(),
                    reminder.remindAt(),
                    CollaborationConstants.STATUS_SENT,
                    triggerAt
            );
            repository.saveMeetingReminder(sent);
            domainEventPublisher.publish(new CollaborationEvents.MeetingReminderDueEvent(
                    UUID.randomUUID(),
                    triggerAt,
                    context.tenantId(),
                    meeting.orElseThrow().meetingId(),
                    reminder.participantId(),
                    meeting.orElseThrow().title(),
                    meeting.orElseThrow().startAt(),
                    "/collaboration?meetingId=" + meeting.orElseThrow().meetingId()
            ));
            sentCount++;
        }
        audit(context, "MEETING_REMINDERS_TRIGGERED", "MEETING_REMINDER", context.tenantId(),
                "scanned=" + dueReminders.size() + ",sent=" + sentCount);
        return new CollaborationViews.ReminderTriggerResult(dueReminders.size(), sentCount);
    }

    public List<CollaborationViews.AuditView> listAuditTrail(CollaborationRequestContext context) {
        return repository.listAuditRecords(context.tenantId(), AUDIT_LIMIT).stream()
                .map(this::toAuditView)
                .toList();
    }

    private CollaborationViews.WorkspaceView doCreateWorkspace(
            CollaborationRequestContext context,
            CollaborationCommands.CreateWorkspaceCommand command
    ) {
        requireText(command.code(), "workspace code");
        requireText(command.name(), "workspace name");
        if (repository.findWorkspaceByCode(context.tenantId(), command.code()).isPresent()) {
            throw new BizException(CollaborationErrorDescriptors.CONFLICT, "Workspace code already exists");
        }
        Instant now = now();
        String workspaceId = id();
        CollaborationRecords.Workspace workspace = repository.saveWorkspace(new CollaborationRecords.Workspace(
                workspaceId,
                context.tenantId(),
                command.code().trim(),
                command.name().trim(),
                trimToNull(command.description()),
                CollaborationConstants.STATUS_ACTIVE,
                defaultText(command.visibility(), "PRIVATE"),
                context.actorId(),
                now,
                now
        ));
        List<CollaborationCommands.MemberCommand> members = new ArrayList<>(nullToList(command.members()));
        members.add(new CollaborationCommands.MemberCommand(
                context.actorId(),
                CollaborationConstants.ROLE_OWNER,
                permissionsForRole(CollaborationConstants.ROLE_OWNER)
        ));
        for (CollaborationCommands.MemberCommand member : dedupeMembers(members)) {
            saveMember(context.tenantId(), workspaceId, member, now);
        }
        audit(context, "WORKSPACE_CREATED", CollaborationConstants.OBJECT_WORKSPACE, workspaceId, workspace.name());
        return toWorkspaceView(workspace);
    }

    private CollaborationViews.DiscussionView doCreateDiscussion(
            CollaborationRequestContext context,
            String workspaceId,
            CollaborationCommands.CreateDiscussionCommand command
    ) {
        requireWorkspacePermission(context, workspaceId, CollaborationConstants.PERMISSION_CREATE_DISCUSSION);
        requireText(command.title(), "discussion title");
        requireText(command.body(), "discussion body");
        Instant now = now();
        CollaborationRecords.Discussion discussion = repository.saveDiscussion(new CollaborationRecords.Discussion(
                id(),
                context.tenantId(),
                workspaceId,
                command.title().trim(),
                command.body().trim(),
                context.actorId(),
                CollaborationConstants.STATUS_OPEN,
                false,
                now,
                now,
                null
        ));
        saveAttachments(context.tenantId(), CollaborationConstants.OBJECT_DISCUSSION, discussion.discussionId(), command.attachments());
        publishMentions(context, workspaceId, CollaborationConstants.OBJECT_DISCUSSION, discussion.discussionId(),
                command.title(), command.mentionPersonIds());
        audit(context, "DISCUSSION_CREATED", CollaborationConstants.OBJECT_DISCUSSION,
                discussion.discussionId(), discussion.title());
        return toDiscussionView(context, discussion);
    }

    private CollaborationViews.DiscussionView doReplyDiscussion(
            CollaborationRequestContext context,
            String discussionId,
            CollaborationCommands.ReplyDiscussionCommand command
    ) {
        CollaborationRecords.Discussion discussion = requireDiscussion(context.tenantId(), discussionId);
        if (CollaborationConstants.STATUS_CLOSED.equals(discussion.status())) {
            throw new BizException(CollaborationErrorDescriptors.INVALID_STATE, "Closed discussion cannot be replied");
        }
        requireWorkspacePermission(context, discussion.workspaceId(), CollaborationConstants.PERMISSION_COMMENT);
        requireText(command.body(), "reply body");
        Instant now = now();
        CollaborationRecords.DiscussionReply reply = repository.saveReply(new CollaborationRecords.DiscussionReply(
                id(),
                context.tenantId(),
                discussionId,
                context.actorId(),
                command.body().trim(),
                false,
                now,
                null
        ));
        saveAttachments(context.tenantId(), "DISCUSSION_REPLY", reply.replyId(), command.attachments());
        publishMentions(context, discussion.workspaceId(), "DISCUSSION_REPLY", reply.replyId(),
                discussion.title(), command.mentionPersonIds());
        CollaborationRecords.Discussion updated = new CollaborationRecords.Discussion(
                discussion.discussionId(),
                discussion.tenantId(),
                discussion.workspaceId(),
                discussion.title(),
                discussion.body(),
                discussion.authorId(),
                discussion.status(),
                discussion.pinned(),
                discussion.createdAt(),
                now,
                discussion.closedAt()
        );
        repository.saveDiscussion(updated);
        audit(context, "DISCUSSION_REPLIED", CollaborationConstants.OBJECT_DISCUSSION, discussionId, reply.replyId());
        return toDiscussionView(context, updated);
    }

    private CollaborationViews.CommentView doCreateComment(
            CollaborationRequestContext context,
            CollaborationCommands.CreateCommentCommand command
    ) {
        String objectType = normalizeCode(command.objectType());
        requireText(command.objectId(), "objectId");
        requireText(command.body(), "comment body");
        String workspaceId = resolveWorkspaceId(context, objectType, command.objectId(), command.workspaceId());
        requireWorkspacePermission(context, workspaceId, CollaborationConstants.PERMISSION_COMMENT);
        Instant now = now();
        CollaborationRecords.Comment comment = repository.saveComment(new CollaborationRecords.Comment(
                id(),
                context.tenantId(),
                workspaceId,
                objectType,
                command.objectId().trim(),
                context.actorId(),
                command.body().trim(),
                false,
                now,
                now,
                null
        ));
        saveAttachments(context.tenantId(), "COMMENT", comment.commentId(), command.attachments());
        publishMentions(context, workspaceId, "COMMENT", comment.commentId(), objectType, command.mentionPersonIds());
        audit(context, "COMMENT_CREATED", "COMMENT", comment.commentId(), objectType + ":" + command.objectId());
        return toCommentView(comment);
    }

    private CollaborationViews.TaskView doCreateTask(
            CollaborationRequestContext context,
            CollaborationCommands.CreateTaskCommand command
    ) {
        requireWorkspacePermission(context, command.workspaceId(), CollaborationConstants.PERMISSION_CREATE_TASK);
        requireText(command.title(), "task title");
        requireText(command.assigneeId(), "assigneeId");
        Instant now = now();
        CollaborationRecords.Task task = repository.saveTask(new CollaborationRecords.Task(
                id(),
                context.tenantId(),
                command.workspaceId(),
                command.title().trim(),
                trimToNull(command.description()),
                context.actorId(),
                command.assigneeId().trim(),
                CollaborationConstants.STATUS_TODO,
                normalizePriority(command.priority()),
                command.dueAt(),
                null,
                now,
                now
        ));
        replaceTaskParticipants(context.tenantId(), task.taskId(), task.creatorId(), task.assigneeId(), command.participantIds());
        domainEventPublisher.publish(new CollaborationEvents.TaskAssignedEvent(
                UUID.randomUUID(),
                now,
                context.tenantId(),
                task.taskId(),
                task.assigneeId(),
                task.title(),
                task.priority(),
                task.dueAt(),
                "/collaboration?taskId=" + task.taskId()
        ));
        audit(context, "TASK_CREATED", CollaborationConstants.OBJECT_TASK, task.taskId(), task.title());
        return toTaskView(context, task);
    }

    private CollaborationViews.TaskView doChangeTaskStatus(
            CollaborationRequestContext context,
            String taskId,
            CollaborationCommands.ChangeTaskStatusCommand command
    ) {
        CollaborationRecords.Task task = requireTask(context.tenantId(), taskId);
        if (!task.creatorId().equals(context.actorId())
                && !task.assigneeId().equals(context.actorId())
                && !hasWorkspacePermission(context, task.workspaceId(), CollaborationConstants.PERMISSION_MANAGE_TASK)) {
            throw new BizException(CollaborationErrorDescriptors.FORBIDDEN, "No permission to change task");
        }
        String newStatus = normalizeCode(command.status());
        if (!CollaborationConstants.TASK_TRANSITIONS.getOrDefault(task.status(), Set.of()).contains(newStatus)) {
            throw new BizException(CollaborationErrorDescriptors.INVALID_STATE,
                    "Task status cannot change from " + task.status() + " to " + newStatus);
        }
        String assigneeId = defaultText(command.assigneeId(), task.assigneeId());
        Instant now = now();
        CollaborationRecords.Task updated = repository.saveTask(new CollaborationRecords.Task(
                task.taskId(),
                task.tenantId(),
                task.workspaceId(),
                task.title(),
                task.description(),
                task.creatorId(),
                assigneeId,
                newStatus,
                task.priority(),
                task.dueAt(),
                CollaborationConstants.STATUS_DONE.equals(newStatus) ? now : null,
                task.createdAt(),
                now
        ));
        if (!assigneeId.equals(task.assigneeId())) {
            replaceTaskParticipants(context.tenantId(), task.taskId(), task.creatorId(), assigneeId,
                    repository.listTaskParticipants(context.tenantId(), task.taskId()).stream()
                            .map(CollaborationRecords.TaskParticipant::personId)
                            .toList());
            domainEventPublisher.publish(new CollaborationEvents.TaskAssignedEvent(
                    UUID.randomUUID(),
                    now,
                    context.tenantId(),
                    task.taskId(),
                    assigneeId,
                    task.title(),
                    task.priority(),
                    task.dueAt(),
                    "/collaboration?taskId=" + task.taskId()
            ));
        }
        notifyTaskParticipants(updated, task.status(), newStatus, now);
        if (command.comment() != null && !command.comment().isBlank()) {
            repository.saveComment(new CollaborationRecords.Comment(
                    id(),
                    context.tenantId(),
                    task.workspaceId(),
                    CollaborationConstants.OBJECT_TASK,
                    task.taskId(),
                    context.actorId(),
                    command.comment().trim(),
                    false,
                    now,
                    now,
                    null
            ));
        }
        audit(context, "TASK_STATUS_CHANGED", CollaborationConstants.OBJECT_TASK, task.taskId(),
                task.status() + "->" + newStatus);
        return toTaskView(context, updated);
    }

    private CollaborationViews.MeetingView doCreateMeeting(
            CollaborationRequestContext context,
            CollaborationCommands.CreateMeetingCommand command
    ) {
        requireWorkspacePermission(context, command.workspaceId(), CollaborationConstants.PERMISSION_CREATE_MEETING);
        requireText(command.title(), "meeting title");
        Objects.requireNonNull(command.startAt(), "startAt must not be null");
        Objects.requireNonNull(command.endAt(), "endAt must not be null");
        if (!command.endAt().isAfter(command.startAt())) {
            throw new BizException(CollaborationErrorDescriptors.INVALID_STATE, "Meeting endAt must be after startAt");
        }
        Instant now = now();
        int reminderMinutes = command.reminderMinutesBefore() <= 0
                ? DEFAULT_REMINDER_MINUTES
                : command.reminderMinutesBefore();
        CollaborationRecords.Meeting meeting = repository.saveMeeting(new CollaborationRecords.Meeting(
                id(),
                context.tenantId(),
                command.workspaceId(),
                command.title().trim(),
                trimToNull(command.agenda()),
                context.actorId(),
                CollaborationConstants.STATUS_SCHEDULED,
                command.startAt(),
                command.endAt(),
                trimToNull(command.location()),
                reminderMinutes,
                null,
                null,
                now,
                now
        ));
        List<String> participants = dedupePersonIds(command.participantIds());
        participants.add(context.actorId());
        replaceMeetingParticipants(context.tenantId(), meeting.meetingId(), context.actorId(), participants);
        createMeetingReminders(context.tenantId(), meeting, participants);
        audit(context, "MEETING_CREATED", CollaborationConstants.OBJECT_MEETING, meeting.meetingId(), meeting.title());
        return toMeetingView(meeting);
    }

    private CollaborationViews.MeetingView doChangeMeetingStatus(
            CollaborationRequestContext context,
            String meetingId,
            CollaborationCommands.ChangeMeetingStatusCommand command
    ) {
        CollaborationRecords.Meeting meeting = requireMeeting(context.tenantId(), meetingId);
        if (!meeting.organizerId().equals(context.actorId())) {
            requireWorkspacePermission(context, meeting.workspaceId(), CollaborationConstants.PERMISSION_MANAGE_MEETING);
        }
        String newStatus = normalizeCode(command.status());
        if (!CollaborationConstants.MEETING_TRANSITIONS.getOrDefault(meeting.status(), Set.of()).contains(newStatus)) {
            throw new BizException(CollaborationErrorDescriptors.INVALID_STATE,
                    "Meeting status cannot change from " + meeting.status() + " to " + newStatus);
        }
        CollaborationRecords.Meeting updated = repository.saveMeeting(new CollaborationRecords.Meeting(
                meeting.meetingId(),
                meeting.tenantId(),
                meeting.workspaceId(),
                meeting.title(),
                meeting.agenda(),
                meeting.organizerId(),
                newStatus,
                meeting.startAt(),
                meeting.endAt(),
                meeting.location(),
                meeting.reminderMinutesBefore(),
                meeting.minutes(),
                meeting.minutesPublishedAt(),
                meeting.createdAt(),
                now()
        ));
        audit(context, "MEETING_STATUS_CHANGED", CollaborationConstants.OBJECT_MEETING, meetingId,
                meeting.status() + "->" + newStatus + ":" + defaultText(command.reason(), ""));
        return toMeetingView(updated);
    }

    private CollaborationViews.MeetingView doPublishMeetingMinutes(
            CollaborationRequestContext context,
            String meetingId,
            CollaborationCommands.PublishMeetingMinutesCommand command
    ) {
        CollaborationRecords.Meeting meeting = requireMeeting(context.tenantId(), meetingId);
        if (!meeting.organizerId().equals(context.actorId())) {
            requireWorkspacePermission(context, meeting.workspaceId(), CollaborationConstants.PERMISSION_MANAGE_MEETING);
        }
        if (!CollaborationConstants.STATUS_COMPLETED.equals(meeting.status())) {
            throw new BizException(CollaborationErrorDescriptors.INVALID_STATE, "Only completed meeting can publish minutes");
        }
        requireText(command.minutes(), "minutes");
        Instant now = now();
        CollaborationRecords.Meeting updated = repository.saveMeeting(new CollaborationRecords.Meeting(
                meeting.meetingId(),
                meeting.tenantId(),
                meeting.workspaceId(),
                meeting.title(),
                meeting.agenda(),
                meeting.organizerId(),
                meeting.status(),
                meeting.startAt(),
                meeting.endAt(),
                meeting.location(),
                meeting.reminderMinutesBefore(),
                command.minutes().trim(),
                now,
                meeting.createdAt(),
                now
        ));
        publishMentions(context, meeting.workspaceId(), "MEETING_MINUTES", meeting.meetingId(),
                meeting.title(), command.mentionPersonIds());
        audit(context, "MEETING_MINUTES_PUBLISHED", CollaborationConstants.OBJECT_MEETING, meetingId, "minutes");
        return toMeetingView(updated);
    }

    private void notifyTaskParticipants(
            CollaborationRecords.Task task,
            String oldStatus,
            String newStatus,
            Instant occurredAt
    ) {
        for (CollaborationRecords.TaskParticipant participant
                : repository.listTaskParticipants(task.tenantId(), task.taskId())) {
            if (participant.personId().equals(task.creatorId()) && !CollaborationConstants.STATUS_DONE.equals(newStatus)) {
                continue;
            }
            domainEventPublisher.publish(new CollaborationEvents.TaskChangedEvent(
                    UUID.randomUUID(),
                    occurredAt,
                    task.tenantId(),
                    task.taskId(),
                    participant.personId(),
                    task.title(),
                    oldStatus,
                    newStatus,
                    "/collaboration?taskId=" + task.taskId()
            ));
        }
    }

    private void publishMentions(
            CollaborationRequestContext context,
            String workspaceId,
            String sourceType,
            String sourceId,
            String title,
            List<String> mentionPersonIds
    ) {
        for (String recipientId : dedupePersonIds(mentionPersonIds)) {
            if (recipientId.equals(context.actorId())) {
                continue;
            }
            domainEventPublisher.publish(new CollaborationEvents.MentionCreatedEvent(
                    UUID.randomUUID(),
                    now(),
                    context.tenantId(),
                    recipientId,
                    context.actorId(),
                    workspaceId,
                    sourceType,
                    sourceId,
                    title,
                    "/collaboration?" + sourceType.toLowerCase() + "Id=" + sourceId
            ));
        }
    }

    private void replaceTaskParticipants(
            String tenantId,
            String taskId,
            String creatorId,
            String assigneeId,
            List<String> participantIds
    ) {
        repository.deleteTaskParticipants(tenantId, taskId);
        List<String> persons = dedupePersonIds(participantIds);
        persons.add(creatorId);
        persons.add(assigneeId);
        for (String personId : new LinkedHashSet<>(persons)) {
            String role = personId.equals(assigneeId) ? "ASSIGNEE" : "PARTICIPANT";
            repository.saveTaskParticipant(new CollaborationRecords.TaskParticipant(
                    id(),
                    tenantId,
                    taskId,
                    personId,
                    role
            ));
        }
    }

    private void replaceMeetingParticipants(
            String tenantId,
            String meetingId,
            String organizerId,
            List<String> participantIds
    ) {
        repository.deleteMeetingParticipants(tenantId, meetingId);
        for (String personId : new LinkedHashSet<>(participantIds)) {
            repository.saveMeetingParticipant(new CollaborationRecords.MeetingParticipant(
                    id(),
                    tenantId,
                    meetingId,
                    personId,
                    personId.equals(organizerId) ? "ORGANIZER" : "ATTENDEE",
                    "NEEDS_ACTION"
            ));
        }
    }

    private void createMeetingReminders(
            String tenantId,
            CollaborationRecords.Meeting meeting,
            List<String> participantIds
    ) {
        Instant remindAt = meeting.startAt().minus(meeting.reminderMinutesBefore(), ChronoUnit.MINUTES);
        for (String participantId : new LinkedHashSet<>(participantIds)) {
            repository.saveMeetingReminder(new CollaborationRecords.MeetingReminder(
                    id(),
                    tenantId,
                    meeting.meetingId(),
                    participantId,
                    remindAt,
                    CollaborationConstants.STATUS_PENDING,
                    null
            ));
        }
    }

    private void saveAttachments(
            String tenantId,
            String ownerType,
            String ownerId,
            List<CollaborationCommands.AttachmentCommand> attachments
    ) {
        for (CollaborationCommands.AttachmentCommand attachment : nullToList(attachments)) {
            requireText(attachment.attachmentId(), "attachmentId");
            repository.saveAttachmentLink(new CollaborationRecords.AttachmentLink(
                    id(),
                    tenantId,
                    ownerType,
                    ownerId,
                    attachment.attachmentId().trim(),
                    defaultText(attachment.fileName(), attachment.attachmentId()),
                    now()
            ));
        }
    }

    private CollaborationViews.WorkspaceView toWorkspaceView(CollaborationRecords.Workspace workspace) {
        return new CollaborationViews.WorkspaceView(
                workspace.workspaceId(),
                workspace.code(),
                workspace.name(),
                workspace.description(),
                workspace.status(),
                workspace.visibility(),
                workspace.ownerId(),
                repository.listMembers(workspace.tenantId(), workspace.workspaceId()).stream()
                        .map(this::toMemberView)
                        .toList(),
                workspace.updatedAt()
        );
    }

    private CollaborationViews.MemberView toMemberView(CollaborationRecords.WorkspaceMember member) {
        return new CollaborationViews.MemberView(
                member.personId(),
                member.roleCode(),
                splitPermissions(member.permissions()),
                member.status()
        );
    }

    private CollaborationViews.DiscussionView toDiscussionView(
            CollaborationRequestContext context,
            CollaborationRecords.Discussion discussion
    ) {
        return new CollaborationViews.DiscussionView(
                discussion.discussionId(),
                discussion.workspaceId(),
                discussion.title(),
                discussion.body(),
                discussion.authorId(),
                discussion.status(),
                discussion.pinned(),
                repository.findRead(context.tenantId(), discussion.discussionId(), context.actorId()).isPresent(),
                repository.listAttachmentLinks(context.tenantId(), CollaborationConstants.OBJECT_DISCUSSION,
                                discussion.discussionId()).stream()
                        .map(this::toAttachmentView)
                        .toList(),
                repository.listReplies(context.tenantId(), discussion.discussionId()).stream()
                        .map(this::toReplyView)
                        .toList(),
                discussion.updatedAt()
        );
    }

    private CollaborationViews.DiscussionReplyView toReplyView(CollaborationRecords.DiscussionReply reply) {
        return new CollaborationViews.DiscussionReplyView(
                reply.replyId(),
                reply.authorId(),
                reply.deleted() ? "" : reply.body(),
                reply.deleted(),
                reply.createdAt()
        );
    }

    private CollaborationViews.CommentView toCommentView(CollaborationRecords.Comment comment) {
        return new CollaborationViews.CommentView(
                comment.commentId(),
                comment.workspaceId(),
                comment.objectType(),
                comment.objectId(),
                comment.authorId(),
                comment.deleted() ? "" : comment.body(),
                comment.deleted(),
                comment.createdAt(),
                comment.updatedAt()
        );
    }

    private CollaborationViews.TaskView toTaskView(
            CollaborationRequestContext context,
            CollaborationRecords.Task task
    ) {
        return new CollaborationViews.TaskView(
                task.taskId(),
                task.workspaceId(),
                task.title(),
                task.description(),
                task.creatorId(),
                task.assigneeId(),
                task.status(),
                task.priority(),
                task.dueAt(),
                task.completedAt(),
                repository.listTaskParticipants(context.tenantId(), task.taskId()).stream()
                        .map(CollaborationRecords.TaskParticipant::personId)
                        .toList(),
                repository.listComments(context.tenantId(), CollaborationConstants.OBJECT_TASK, task.taskId()).stream()
                        .map(this::toCommentView)
                        .toList(),
                task.updatedAt()
        );
    }

    private CollaborationViews.MeetingView toMeetingView(CollaborationRecords.Meeting meeting) {
        return new CollaborationViews.MeetingView(
                meeting.meetingId(),
                meeting.workspaceId(),
                meeting.title(),
                meeting.agenda(),
                meeting.organizerId(),
                meeting.status(),
                meeting.startAt(),
                meeting.endAt(),
                meeting.location(),
                meeting.reminderMinutesBefore(),
                meeting.minutes(),
                meeting.minutesPublishedAt(),
                repository.listMeetingParticipants(meeting.tenantId(), meeting.meetingId()).stream()
                        .map(CollaborationRecords.MeetingParticipant::personId)
                        .toList(),
                meeting.updatedAt()
        );
    }

    private CollaborationViews.AttachmentView toAttachmentView(CollaborationRecords.AttachmentLink attachmentLink) {
        return new CollaborationViews.AttachmentView(attachmentLink.attachmentId(), attachmentLink.fileName());
    }

    private CollaborationViews.AuditView toAuditView(CollaborationRecords.AuditRecord auditRecord) {
        return new CollaborationViews.AuditView(
                auditRecord.auditId(),
                auditRecord.actorId(),
                auditRecord.actionCode(),
                auditRecord.resourceType(),
                auditRecord.resourceId(),
                auditRecord.requestId(),
                auditRecord.detail(),
                auditRecord.occurredAt()
        );
    }

    private CollaborationRecords.Workspace requireWorkspace(String tenantId, String workspaceId) {
        return repository.findWorkspace(tenantId, workspaceId)
                .orElseThrow(() -> new BizException(CollaborationErrorDescriptors.NOT_FOUND, "Workspace not found"));
    }

    private CollaborationRecords.Workspace requireVisibleWorkspace(
            CollaborationRequestContext context,
            String workspaceId
    ) {
        CollaborationRecords.Workspace workspace = requireWorkspace(context.tenantId(), workspaceId);
        if (!isWorkspaceVisible(workspace, context.actorId())) {
            throw new BizException(CollaborationErrorDescriptors.FORBIDDEN, "No permission to workspace");
        }
        return workspace;
    }

    private CollaborationRecords.Discussion requireDiscussion(String tenantId, String discussionId) {
        return repository.findDiscussion(tenantId, discussionId)
                .orElseThrow(() -> new BizException(CollaborationErrorDescriptors.NOT_FOUND, "Discussion not found"));
    }

    private CollaborationRecords.Comment requireComment(String tenantId, String commentId) {
        return repository.findComment(tenantId, commentId)
                .orElseThrow(() -> new BizException(CollaborationErrorDescriptors.NOT_FOUND, "Comment not found"));
    }

    private CollaborationRecords.Task requireTask(String tenantId, String taskId) {
        return repository.findTask(tenantId, taskId)
                .orElseThrow(() -> new BizException(CollaborationErrorDescriptors.NOT_FOUND, "Task not found"));
    }

    private CollaborationRecords.Meeting requireMeeting(String tenantId, String meetingId) {
        return repository.findMeeting(tenantId, meetingId)
                .orElseThrow(() -> new BizException(CollaborationErrorDescriptors.NOT_FOUND, "Meeting not found"));
    }

    private boolean isWorkspaceVisible(CollaborationRecords.Workspace workspace, String actorId) {
        return workspace.ownerId().equals(actorId)
                || repository.findMember(workspace.tenantId(), workspace.workspaceId(), actorId)
                        .filter(member -> CollaborationConstants.STATUS_ACTIVE.equals(member.status()))
                        .isPresent();
    }

    private boolean taskVisibleTo(CollaborationRecords.Task task, String actorId) {
        return task.creatorId().equals(actorId)
                || task.assigneeId().equals(actorId)
                || repository.listTaskParticipants(task.tenantId(), task.taskId()).stream()
                        .anyMatch(participant -> participant.personId().equals(actorId));
    }

    private boolean meetingVisibleTo(CollaborationRecords.Meeting meeting, String actorId) {
        return meeting.organizerId().equals(actorId)
                || repository.listMeetingParticipants(meeting.tenantId(), meeting.meetingId()).stream()
                        .anyMatch(participant -> participant.personId().equals(actorId));
    }

    private void requireWorkspacePermission(
            CollaborationRequestContext context,
            String workspaceId,
            String permission
    ) {
        if (!hasWorkspacePermission(context, workspaceId, permission)) {
            throw new BizException(CollaborationErrorDescriptors.FORBIDDEN, "No permission: " + permission);
        }
    }

    private boolean hasWorkspacePermission(
            CollaborationRequestContext context,
            String workspaceId,
            String permission
    ) {
        CollaborationRecords.Workspace workspace = requireWorkspace(context.tenantId(), workspaceId);
        if (workspace.ownerId().equals(context.actorId())) {
            return true;
        }
        return repository.findMember(context.tenantId(), workspaceId, context.actorId())
                .filter(member -> CollaborationConstants.STATUS_ACTIVE.equals(member.status()))
                .map(member -> splitPermissions(member.permissions()).contains(permission))
                .orElse(false);
    }

    private String resolveWorkspaceId(
            CollaborationRequestContext context,
            String objectType,
            String objectId,
            String requestedWorkspaceId
    ) {
        if (CollaborationConstants.OBJECT_TASK.equals(objectType)) {
            return requireTask(context.tenantId(), objectId).workspaceId();
        }
        if (CollaborationConstants.OBJECT_MEETING.equals(objectType)) {
            return requireMeeting(context.tenantId(), objectId).workspaceId();
        }
        if (CollaborationConstants.OBJECT_DISCUSSION.equals(objectType)) {
            return requireDiscussion(context.tenantId(), objectId).workspaceId();
        }
        if (CollaborationConstants.OBJECT_WORKSPACE.equals(objectType)) {
            return objectId;
        }
        requireText(requestedWorkspaceId, "workspaceId");
        return requestedWorkspaceId.trim();
    }

    private <T> T withIdempotency(
            CollaborationRequestContext context,
            String operation,
            String resourceType,
            Function<String, T> existingResolver,
            Supplier<T> creator
    ) {
        requireIdempotencyKey(context.idempotencyKey());
        Optional<CollaborationRecords.IdempotencyRecord> existing =
                repository.findIdempotency(context.tenantId(), operation, context.idempotencyKey());
        if (existing.isPresent()) {
            return existingResolver.apply(existing.orElseThrow().resourceId());
        }
        T created = creator.get();
        String resourceId = resolveCreatedResourceId(created);
        repository.saveIdempotency(new CollaborationRecords.IdempotencyRecord(
                id(),
                context.tenantId(),
                operation,
                context.idempotencyKey(),
                resourceType,
                resourceId,
                now()
        ));
        return created;
    }

    private String resolveCreatedResourceId(Object created) {
        if (created instanceof CollaborationViews.WorkspaceView view) {
            return view.workspaceId();
        }
        if (created instanceof CollaborationViews.DiscussionView view) {
            return view.discussionId();
        }
        if (created instanceof CollaborationViews.CommentView view) {
            return view.commentId();
        }
        if (created instanceof CollaborationViews.TaskView view) {
            return view.taskId();
        }
        if (created instanceof CollaborationViews.MeetingView view) {
            return view.meetingId();
        }
        throw new IllegalArgumentException("Unsupported idempotent response " + created.getClass().getName());
    }

    private List<CollaborationCommands.MemberCommand> dedupeMembers(
            List<CollaborationCommands.MemberCommand> members
    ) {
        List<CollaborationCommands.MemberCommand> result = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (CollaborationCommands.MemberCommand member : nullToList(members)) {
            requireText(member.personId(), "member personId");
            if (seen.add(member.personId().trim())) {
                String role = normalizeRole(member.roleCode());
                List<String> permissions = member.permissions() == null || member.permissions().isEmpty()
                        ? permissionsForRole(role)
                        : member.permissions();
                result.add(new CollaborationCommands.MemberCommand(member.personId().trim(), role, permissions));
            }
        }
        return result;
    }

    private void saveMember(String tenantId, String workspaceId, CollaborationCommands.MemberCommand member, Instant now) {
        repository.saveMember(new CollaborationRecords.WorkspaceMember(
                id(),
                tenantId,
                workspaceId,
                member.personId().trim(),
                normalizeRole(member.roleCode()),
                String.join(",", member.permissions()),
                CollaborationConstants.STATUS_ACTIVE,
                now,
                now
        ));
    }

    private List<String> permissionsForRole(String role) {
        return switch (normalizeRole(role)) {
            case CollaborationConstants.ROLE_OWNER -> List.of(
                    CollaborationConstants.PERMISSION_READ,
                    CollaborationConstants.PERMISSION_COMMENT,
                    CollaborationConstants.PERMISSION_CREATE_DISCUSSION,
                    CollaborationConstants.PERMISSION_MANAGE_DISCUSSION,
                    CollaborationConstants.PERMISSION_CREATE_TASK,
                    CollaborationConstants.PERMISSION_MANAGE_TASK,
                    CollaborationConstants.PERMISSION_CREATE_MEETING,
                    CollaborationConstants.PERMISSION_MANAGE_MEETING,
                    CollaborationConstants.PERMISSION_MANAGE_MEMBERS
            );
            case CollaborationConstants.ROLE_MANAGER -> List.of(
                    CollaborationConstants.PERMISSION_READ,
                    CollaborationConstants.PERMISSION_COMMENT,
                    CollaborationConstants.PERMISSION_CREATE_DISCUSSION,
                    CollaborationConstants.PERMISSION_MANAGE_DISCUSSION,
                    CollaborationConstants.PERMISSION_CREATE_TASK,
                    CollaborationConstants.PERMISSION_MANAGE_TASK,
                    CollaborationConstants.PERMISSION_CREATE_MEETING,
                    CollaborationConstants.PERMISSION_MANAGE_MEETING,
                    CollaborationConstants.PERMISSION_MANAGE_MEMBERS
            );
            case CollaborationConstants.ROLE_MEMBER -> List.of(
                    CollaborationConstants.PERMISSION_READ,
                    CollaborationConstants.PERMISSION_COMMENT,
                    CollaborationConstants.PERMISSION_CREATE_DISCUSSION,
                    CollaborationConstants.PERMISSION_CREATE_TASK,
                    CollaborationConstants.PERMISSION_CREATE_MEETING
            );
            default -> List.of(CollaborationConstants.PERMISSION_READ);
        };
    }

    private void audit(
            CollaborationRequestContext context,
            String action,
            String resourceType,
            String resourceId,
            String detail
    ) {
        repository.saveAudit(new CollaborationRecords.AuditRecord(
                id(),
                context.tenantId(),
                context.actorId(),
                action,
                resourceType,
                resourceId,
                context.requestId(),
                trimToNull(detail),
                now()
        ));
    }

    private Instant now() {
        return clock.instant();
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }

    private static String normalizeRole(String role) {
        String value = normalizeCode(defaultText(role, CollaborationConstants.ROLE_MEMBER));
        if (Set.of(
                CollaborationConstants.ROLE_OWNER,
                CollaborationConstants.ROLE_MANAGER,
                CollaborationConstants.ROLE_MEMBER,
                CollaborationConstants.ROLE_VIEWER
        ).contains(value)) {
            return value;
        }
        return CollaborationConstants.ROLE_MEMBER;
    }

    private static String normalizePriority(String priority) {
        String value = normalizeCode(defaultText(priority, CollaborationConstants.PRIORITY_NORMAL));
        if (Set.of(
                CollaborationConstants.PRIORITY_LOW,
                CollaborationConstants.PRIORITY_NORMAL,
                CollaborationConstants.PRIORITY_HIGH,
                CollaborationConstants.PRIORITY_URGENT
        ).contains(value)) {
            return value;
        }
        return CollaborationConstants.PRIORITY_NORMAL;
    }

    private static String normalizeCode(String value) {
        requireText(value, "code");
        return value.trim().toUpperCase();
    }

    private static List<String> splitPermissions(String permissions) {
        if (permissions == null || permissions.isBlank()) {
            return List.of();
        }
        return List.of(permissions.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private static List<String> dedupePersonIds(List<String> personIds) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String personId : nullToList(personIds)) {
            if (personId != null && !personId.isBlank()) {
                seen.add(personId.trim());
            }
        }
        return new ArrayList<>(seen);
    }

    private static <T> List<T> nullToList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BizException(CollaborationErrorDescriptors.BAD_REQUEST, fieldName + " must not be blank");
        }
    }

    private static void requireIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException(CollaborationErrorDescriptors.IDEMPOTENCY_REQUIRED, "X-Idempotency-Key is required");
        }
    }
}
