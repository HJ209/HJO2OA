package com.hjo2oa.biz.collaboration.hub.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjo2oa.biz.collaboration.hub.domain.CollaborationConstants;
import com.hjo2oa.biz.collaboration.hub.domain.CollaborationRecords;
import com.hjo2oa.biz.collaboration.hub.domain.CollaborationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisCollaborationRepository implements CollaborationRepository {

    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberMapper memberMapper;
    private final DiscussionMapper discussionMapper;
    private final DiscussionReplyMapper replyMapper;
    private final DiscussionReadMapper readMapper;
    private final CommentMapper commentMapper;
    private final TaskMapper taskMapper;
    private final TaskParticipantMapper taskParticipantMapper;
    private final MeetingMapper meetingMapper;
    private final MeetingParticipantMapper meetingParticipantMapper;
    private final MeetingReminderMapper meetingReminderMapper;
    private final AttachmentLinkMapper attachmentLinkMapper;
    private final CollaborationAuditRecordMapper auditRecordMapper;
    private final IdempotencyRecordMapper idempotencyRecordMapper;

    @SuppressWarnings("ParameterNumber")
    public MybatisCollaborationRepository(
            WorkspaceMapper workspaceMapper,
            WorkspaceMemberMapper memberMapper,
            DiscussionMapper discussionMapper,
            DiscussionReplyMapper replyMapper,
            DiscussionReadMapper readMapper,
            CommentMapper commentMapper,
            TaskMapper taskMapper,
            TaskParticipantMapper taskParticipantMapper,
            MeetingMapper meetingMapper,
            MeetingParticipantMapper meetingParticipantMapper,
            MeetingReminderMapper meetingReminderMapper,
            AttachmentLinkMapper attachmentLinkMapper,
            CollaborationAuditRecordMapper auditRecordMapper,
            IdempotencyRecordMapper idempotencyRecordMapper
    ) {
        this.workspaceMapper = workspaceMapper;
        this.memberMapper = memberMapper;
        this.discussionMapper = discussionMapper;
        this.replyMapper = replyMapper;
        this.readMapper = readMapper;
        this.commentMapper = commentMapper;
        this.taskMapper = taskMapper;
        this.taskParticipantMapper = taskParticipantMapper;
        this.meetingMapper = meetingMapper;
        this.meetingParticipantMapper = meetingParticipantMapper;
        this.meetingReminderMapper = meetingReminderMapper;
        this.attachmentLinkMapper = attachmentLinkMapper;
        this.auditRecordMapper = auditRecordMapper;
        this.idempotencyRecordMapper = idempotencyRecordMapper;
    }

    @Override
    public CollaborationRecords.Workspace saveWorkspace(CollaborationRecords.Workspace workspace) {
        WorkspaceEntity entity = toWorkspaceEntity(workspace);
        if (workspaceMapper.selectById(workspace.workspaceId()) == null) {
            workspaceMapper.insert(entity);
        } else {
            workspaceMapper.updateById(entity);
        }
        return findWorkspace(workspace.tenantId(), workspace.workspaceId()).orElseThrow();
    }

    @Override
    public Optional<CollaborationRecords.Workspace> findWorkspace(String tenantId, String workspaceId) {
        return Optional.ofNullable(workspaceMapper.selectById(workspaceId))
                .filter(entity -> entity.getTenantId().equals(tenantId))
                .map(this::toWorkspace);
    }

    @Override
    public Optional<CollaborationRecords.Workspace> findWorkspaceByCode(String tenantId, String code) {
        return Optional.ofNullable(workspaceMapper.selectOne(new QueryWrapper<WorkspaceEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("code", code)))
                .map(this::toWorkspace);
    }

    @Override
    public List<CollaborationRecords.Workspace> listWorkspaces(String tenantId) {
        return workspaceMapper.selectList(new QueryWrapper<WorkspaceEntity>()
                        .eq("tenant_id", tenantId)
                        .orderByDesc("updated_at"))
                .stream()
                .map(this::toWorkspace)
                .toList();
    }

    @Override
    public CollaborationRecords.WorkspaceMember saveMember(CollaborationRecords.WorkspaceMember member) {
        findMember(member.tenantId(), member.workspaceId(), member.personId())
                .map(CollaborationRecords.WorkspaceMember::memberId)
                .ifPresent(memberMapper::deleteById);
        memberMapper.insert(toMemberEntity(member));
        return findMember(member.tenantId(), member.workspaceId(), member.personId()).orElseThrow();
    }

    @Override
    public void deleteMembersByWorkspace(String tenantId, String workspaceId) {
        memberMapper.delete(new QueryWrapper<WorkspaceMemberEntity>()
                .eq("tenant_id", tenantId)
                .eq("workspace_id", workspaceId));
    }

    @Override
    public List<CollaborationRecords.WorkspaceMember> listMembers(String tenantId, String workspaceId) {
        return memberMapper.selectList(new QueryWrapper<WorkspaceMemberEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("workspace_id", workspaceId))
                .stream()
                .map(this::toMember)
                .toList();
    }

    @Override
    public Optional<CollaborationRecords.WorkspaceMember> findMember(
            String tenantId,
            String workspaceId,
            String personId
    ) {
        return Optional.ofNullable(memberMapper.selectOne(new QueryWrapper<WorkspaceMemberEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("workspace_id", workspaceId)
                        .eq("person_id", personId)))
                .map(this::toMember);
    }

    @Override
    public List<CollaborationRecords.WorkspaceMember> listMembershipsByPerson(String tenantId, String personId) {
        return memberMapper.selectList(new QueryWrapper<WorkspaceMemberEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("person_id", personId))
                .stream()
                .map(this::toMember)
                .toList();
    }

    @Override
    public CollaborationRecords.Discussion saveDiscussion(CollaborationRecords.Discussion discussion) {
        DiscussionEntity entity = toDiscussionEntity(discussion);
        if (discussionMapper.selectById(discussion.discussionId()) == null) {
            discussionMapper.insert(entity);
        } else {
            discussionMapper.updateById(entity);
        }
        return findDiscussion(discussion.tenantId(), discussion.discussionId()).orElseThrow();
    }

    @Override
    public Optional<CollaborationRecords.Discussion> findDiscussion(String tenantId, String discussionId) {
        return Optional.ofNullable(discussionMapper.selectById(discussionId))
                .filter(entity -> entity.getTenantId().equals(tenantId))
                .map(this::toDiscussion);
    }

    @Override
    public List<CollaborationRecords.Discussion> listDiscussions(String tenantId, String workspaceId) {
        return discussionMapper.selectList(new QueryWrapper<DiscussionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("workspace_id", workspaceId)
                        .orderByDesc("pinned")
                        .orderByDesc("updated_at"))
                .stream()
                .map(this::toDiscussion)
                .toList();
    }

    @Override
    public CollaborationRecords.DiscussionReply saveReply(CollaborationRecords.DiscussionReply reply) {
        DiscussionReplyEntity entity = toReplyEntity(reply);
        if (replyMapper.selectById(reply.replyId()) == null) {
            replyMapper.insert(entity);
        } else {
            replyMapper.updateById(entity);
        }
        return entityToReply(replyMapper.selectById(reply.replyId()));
    }

    @Override
    public List<CollaborationRecords.DiscussionReply> listReplies(String tenantId, String discussionId) {
        return replyMapper.selectList(new QueryWrapper<DiscussionReplyEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("discussion_id", discussionId)
                        .orderByAsc("created_at"))
                .stream()
                .map(this::entityToReply)
                .toList();
    }

    @Override
    public CollaborationRecords.DiscussionRead saveRead(CollaborationRecords.DiscussionRead read) {
        findRead(read.tenantId(), read.discussionId(), read.personId())
                .map(CollaborationRecords.DiscussionRead::readId)
                .ifPresent(readMapper::deleteById);
        readMapper.insert(toReadEntity(read));
        return findRead(read.tenantId(), read.discussionId(), read.personId()).orElseThrow();
    }

    @Override
    public Optional<CollaborationRecords.DiscussionRead> findRead(
            String tenantId,
            String discussionId,
            String personId
    ) {
        return Optional.ofNullable(readMapper.selectOne(new QueryWrapper<DiscussionReadEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("discussion_id", discussionId)
                        .eq("person_id", personId)))
                .map(this::toRead);
    }

    @Override
    public CollaborationRecords.Comment saveComment(CollaborationRecords.Comment comment) {
        CommentEntity entity = toCommentEntity(comment);
        if (commentMapper.selectById(comment.commentId()) == null) {
            commentMapper.insert(entity);
        } else {
            commentMapper.updateById(entity);
        }
        return findComment(comment.tenantId(), comment.commentId()).orElseThrow();
    }

    @Override
    public Optional<CollaborationRecords.Comment> findComment(String tenantId, String commentId) {
        return Optional.ofNullable(commentMapper.selectById(commentId))
                .filter(entity -> entity.getTenantId().equals(tenantId))
                .map(this::toComment);
    }

    @Override
    public List<CollaborationRecords.Comment> listComments(String tenantId, String objectType, String objectId) {
        return commentMapper.selectList(new QueryWrapper<CommentEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("object_type", objectType)
                        .eq("object_id", objectId)
                        .orderByAsc("created_at"))
                .stream()
                .map(this::toComment)
                .toList();
    }

    @Override
    public CollaborationRecords.Task saveTask(CollaborationRecords.Task task) {
        TaskEntity entity = toTaskEntity(task);
        if (taskMapper.selectById(task.taskId()) == null) {
            taskMapper.insert(entity);
        } else {
            taskMapper.updateById(entity);
        }
        return findTask(task.tenantId(), task.taskId()).orElseThrow();
    }

    @Override
    public Optional<CollaborationRecords.Task> findTask(String tenantId, String taskId) {
        return Optional.ofNullable(taskMapper.selectById(taskId))
                .filter(entity -> entity.getTenantId().equals(tenantId))
                .map(this::toTask);
    }

    @Override
    public List<CollaborationRecords.Task> listTasks(String tenantId) {
        return taskMapper.selectList(new QueryWrapper<TaskEntity>()
                        .eq("tenant_id", tenantId)
                        .orderByDesc("updated_at"))
                .stream()
                .map(this::toTask)
                .toList();
    }

    @Override
    public CollaborationRecords.TaskParticipant saveTaskParticipant(CollaborationRecords.TaskParticipant participant) {
        taskParticipantMapper.insert(toTaskParticipantEntity(participant));
        return participant;
    }

    @Override
    public void deleteTaskParticipants(String tenantId, String taskId) {
        taskParticipantMapper.delete(new QueryWrapper<TaskParticipantEntity>()
                .eq("tenant_id", tenantId)
                .eq("task_id", taskId));
    }

    @Override
    public List<CollaborationRecords.TaskParticipant> listTaskParticipants(String tenantId, String taskId) {
        return taskParticipantMapper.selectList(new QueryWrapper<TaskParticipantEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("task_id", taskId))
                .stream()
                .map(this::toTaskParticipant)
                .toList();
    }

    @Override
    public CollaborationRecords.Meeting saveMeeting(CollaborationRecords.Meeting meeting) {
        MeetingEntity entity = toMeetingEntity(meeting);
        if (meetingMapper.selectById(meeting.meetingId()) == null) {
            meetingMapper.insert(entity);
        } else {
            meetingMapper.updateById(entity);
        }
        return findMeeting(meeting.tenantId(), meeting.meetingId()).orElseThrow();
    }

    @Override
    public Optional<CollaborationRecords.Meeting> findMeeting(String tenantId, String meetingId) {
        return Optional.ofNullable(meetingMapper.selectById(meetingId))
                .filter(entity -> entity.getTenantId().equals(tenantId))
                .map(this::toMeeting);
    }

    @Override
    public List<CollaborationRecords.Meeting> listMeetings(String tenantId) {
        return meetingMapper.selectList(new QueryWrapper<MeetingEntity>()
                        .eq("tenant_id", tenantId)
                        .orderByAsc("start_at"))
                .stream()
                .map(this::toMeeting)
                .toList();
    }

    @Override
    public CollaborationRecords.MeetingParticipant saveMeetingParticipant(
            CollaborationRecords.MeetingParticipant participant
    ) {
        meetingParticipantMapper.insert(toMeetingParticipantEntity(participant));
        return participant;
    }

    @Override
    public void deleteMeetingParticipants(String tenantId, String meetingId) {
        meetingParticipantMapper.delete(new QueryWrapper<MeetingParticipantEntity>()
                .eq("tenant_id", tenantId)
                .eq("meeting_id", meetingId));
    }

    @Override
    public List<CollaborationRecords.MeetingParticipant> listMeetingParticipants(String tenantId, String meetingId) {
        return meetingParticipantMapper.selectList(new QueryWrapper<MeetingParticipantEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("meeting_id", meetingId))
                .stream()
                .map(this::toMeetingParticipant)
                .toList();
    }

    @Override
    public CollaborationRecords.MeetingReminder saveMeetingReminder(CollaborationRecords.MeetingReminder reminder) {
        MeetingReminderEntity entity = toMeetingReminderEntity(reminder);
        if (meetingReminderMapper.selectById(reminder.reminderId()) == null) {
            meetingReminderMapper.insert(entity);
        } else {
            meetingReminderMapper.updateById(entity);
        }
        return reminder;
    }

    @Override
    public List<CollaborationRecords.MeetingReminder> listDueMeetingReminders(String tenantId, Instant dueAt) {
        return meetingReminderMapper.selectList(new QueryWrapper<MeetingReminderEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("status", CollaborationConstants.STATUS_PENDING)
                        .le("remind_at", dueAt))
                .stream()
                .map(this::toMeetingReminder)
                .toList();
    }

    @Override
    public List<CollaborationRecords.MeetingReminder> listMeetingReminders(String tenantId, String meetingId) {
        return meetingReminderMapper.selectList(new QueryWrapper<MeetingReminderEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("meeting_id", meetingId))
                .stream()
                .map(this::toMeetingReminder)
                .toList();
    }

    @Override
    public CollaborationRecords.AttachmentLink saveAttachmentLink(CollaborationRecords.AttachmentLink attachmentLink) {
        attachmentLinkMapper.insert(toAttachmentLinkEntity(attachmentLink));
        return attachmentLink;
    }

    @Override
    public List<CollaborationRecords.AttachmentLink> listAttachmentLinks(
            String tenantId,
            String ownerType,
            String ownerId
    ) {
        return attachmentLinkMapper.selectList(new QueryWrapper<AttachmentLinkEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("owner_type", ownerType)
                        .eq("owner_id", ownerId))
                .stream()
                .map(this::toAttachmentLink)
                .toList();
    }

    @Override
    public CollaborationRecords.AuditRecord saveAudit(CollaborationRecords.AuditRecord auditRecord) {
        auditRecordMapper.insert(toAuditRecordEntity(auditRecord));
        return auditRecord;
    }

    @Override
    public List<CollaborationRecords.AuditRecord> listAuditRecords(String tenantId, int limit) {
        return auditRecordMapper.selectList(new QueryWrapper<AuditRecordEntity>()
                        .eq("tenant_id", tenantId)
                        .orderByDesc("occurred_at")
                        .last("OFFSET 0 ROWS FETCH NEXT " + limit + " ROWS ONLY"))
                .stream()
                .map(this::toAuditRecord)
                .toList();
    }

    @Override
    public Optional<CollaborationRecords.IdempotencyRecord> findIdempotency(
            String tenantId,
            String operationCode,
            String idempotencyKey
    ) {
        return Optional.ofNullable(idempotencyRecordMapper.selectOne(new QueryWrapper<IdempotencyRecordEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("operation_code", operationCode)
                        .eq("idempotency_key", idempotencyKey)))
                .map(this::toIdempotencyRecord);
    }

    @Override
    public CollaborationRecords.IdempotencyRecord saveIdempotency(
            CollaborationRecords.IdempotencyRecord idempotencyRecord
    ) {
        idempotencyRecordMapper.insert(toIdempotencyRecordEntity(idempotencyRecord));
        return idempotencyRecord;
    }

    private WorkspaceEntity toWorkspaceEntity(CollaborationRecords.Workspace workspace) {
        WorkspaceEntity entity = new WorkspaceEntity();
        entity.setWorkspaceId(workspace.workspaceId());
        entity.setTenantId(workspace.tenantId());
        entity.setCode(workspace.code());
        entity.setName(workspace.name());
        entity.setDescription(workspace.description());
        entity.setStatus(workspace.status());
        entity.setVisibility(workspace.visibility());
        entity.setOwnerId(workspace.ownerId());
        entity.setCreatedAt(workspace.createdAt());
        entity.setUpdatedAt(workspace.updatedAt());
        return entity;
    }

    private CollaborationRecords.Workspace toWorkspace(WorkspaceEntity entity) {
        return new CollaborationRecords.Workspace(entity.getWorkspaceId(), entity.getTenantId(), entity.getCode(),
                entity.getName(), entity.getDescription(), entity.getStatus(), entity.getVisibility(),
                entity.getOwnerId(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private WorkspaceMemberEntity toMemberEntity(CollaborationRecords.WorkspaceMember member) {
        WorkspaceMemberEntity entity = new WorkspaceMemberEntity();
        entity.setMemberId(member.memberId());
        entity.setTenantId(member.tenantId());
        entity.setWorkspaceId(member.workspaceId());
        entity.setPersonId(member.personId());
        entity.setRoleCode(member.roleCode());
        entity.setPermissions(member.permissions());
        entity.setStatus(member.status());
        entity.setJoinedAt(member.joinedAt());
        entity.setUpdatedAt(member.updatedAt());
        return entity;
    }

    private CollaborationRecords.WorkspaceMember toMember(WorkspaceMemberEntity entity) {
        return new CollaborationRecords.WorkspaceMember(entity.getMemberId(), entity.getTenantId(),
                entity.getWorkspaceId(), entity.getPersonId(), entity.getRoleCode(), entity.getPermissions(),
                entity.getStatus(), entity.getJoinedAt(), entity.getUpdatedAt());
    }

    private DiscussionEntity toDiscussionEntity(CollaborationRecords.Discussion discussion) {
        DiscussionEntity entity = new DiscussionEntity();
        entity.setDiscussionId(discussion.discussionId());
        entity.setTenantId(discussion.tenantId());
        entity.setWorkspaceId(discussion.workspaceId());
        entity.setTitle(discussion.title());
        entity.setBody(discussion.body());
        entity.setAuthorId(discussion.authorId());
        entity.setStatus(discussion.status());
        entity.setPinned(discussion.pinned());
        entity.setCreatedAt(discussion.createdAt());
        entity.setUpdatedAt(discussion.updatedAt());
        entity.setClosedAt(discussion.closedAt());
        return entity;
    }

    private CollaborationRecords.Discussion toDiscussion(DiscussionEntity entity) {
        return new CollaborationRecords.Discussion(entity.getDiscussionId(), entity.getTenantId(),
                entity.getWorkspaceId(), entity.getTitle(), entity.getBody(), entity.getAuthorId(),
                entity.getStatus(), Boolean.TRUE.equals(entity.getPinned()), entity.getCreatedAt(),
                entity.getUpdatedAt(), entity.getClosedAt());
    }

    private DiscussionReplyEntity toReplyEntity(CollaborationRecords.DiscussionReply reply) {
        DiscussionReplyEntity entity = new DiscussionReplyEntity();
        entity.setReplyId(reply.replyId());
        entity.setTenantId(reply.tenantId());
        entity.setDiscussionId(reply.discussionId());
        entity.setAuthorId(reply.authorId());
        entity.setBody(reply.body());
        entity.setDeleted(reply.deleted());
        entity.setCreatedAt(reply.createdAt());
        entity.setDeletedAt(reply.deletedAt());
        return entity;
    }

    private CollaborationRecords.DiscussionReply entityToReply(DiscussionReplyEntity entity) {
        return new CollaborationRecords.DiscussionReply(entity.getReplyId(), entity.getTenantId(),
                entity.getDiscussionId(), entity.getAuthorId(), entity.getBody(),
                Boolean.TRUE.equals(entity.getDeleted()), entity.getCreatedAt(), entity.getDeletedAt());
    }

    private DiscussionReadEntity toReadEntity(CollaborationRecords.DiscussionRead read) {
        DiscussionReadEntity entity = new DiscussionReadEntity();
        entity.setReadId(read.readId());
        entity.setTenantId(read.tenantId());
        entity.setDiscussionId(read.discussionId());
        entity.setPersonId(read.personId());
        entity.setReadAt(read.readAt());
        return entity;
    }

    private CollaborationRecords.DiscussionRead toRead(DiscussionReadEntity entity) {
        return new CollaborationRecords.DiscussionRead(entity.getReadId(), entity.getTenantId(),
                entity.getDiscussionId(), entity.getPersonId(), entity.getReadAt());
    }

    private CommentEntity toCommentEntity(CollaborationRecords.Comment comment) {
        CommentEntity entity = new CommentEntity();
        entity.setCommentId(comment.commentId());
        entity.setTenantId(comment.tenantId());
        entity.setWorkspaceId(comment.workspaceId());
        entity.setObjectType(comment.objectType());
        entity.setObjectId(comment.objectId());
        entity.setAuthorId(comment.authorId());
        entity.setBody(comment.body());
        entity.setDeleted(comment.deleted());
        entity.setCreatedAt(comment.createdAt());
        entity.setUpdatedAt(comment.updatedAt());
        entity.setDeletedAt(comment.deletedAt());
        return entity;
    }

    private CollaborationRecords.Comment toComment(CommentEntity entity) {
        return new CollaborationRecords.Comment(entity.getCommentId(), entity.getTenantId(), entity.getWorkspaceId(),
                entity.getObjectType(), entity.getObjectId(), entity.getAuthorId(), entity.getBody(),
                Boolean.TRUE.equals(entity.getDeleted()), entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getDeletedAt());
    }

    private TaskEntity toTaskEntity(CollaborationRecords.Task task) {
        TaskEntity entity = new TaskEntity();
        entity.setTaskId(task.taskId());
        entity.setTenantId(task.tenantId());
        entity.setWorkspaceId(task.workspaceId());
        entity.setTitle(task.title());
        entity.setDescription(task.description());
        entity.setCreatorId(task.creatorId());
        entity.setAssigneeId(task.assigneeId());
        entity.setStatus(task.status());
        entity.setPriority(task.priority());
        entity.setDueAt(task.dueAt());
        entity.setCompletedAt(task.completedAt());
        entity.setCreatedAt(task.createdAt());
        entity.setUpdatedAt(task.updatedAt());
        return entity;
    }

    private CollaborationRecords.Task toTask(TaskEntity entity) {
        return new CollaborationRecords.Task(entity.getTaskId(), entity.getTenantId(), entity.getWorkspaceId(),
                entity.getTitle(), entity.getDescription(), entity.getCreatorId(), entity.getAssigneeId(),
                entity.getStatus(), entity.getPriority(), entity.getDueAt(), entity.getCompletedAt(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private TaskParticipantEntity toTaskParticipantEntity(CollaborationRecords.TaskParticipant participant) {
        TaskParticipantEntity entity = new TaskParticipantEntity();
        entity.setParticipantId(participant.participantId());
        entity.setTenantId(participant.tenantId());
        entity.setTaskId(participant.taskId());
        entity.setPersonId(participant.personId());
        entity.setRoleCode(participant.roleCode());
        return entity;
    }

    private CollaborationRecords.TaskParticipant toTaskParticipant(TaskParticipantEntity entity) {
        return new CollaborationRecords.TaskParticipant(entity.getParticipantId(), entity.getTenantId(),
                entity.getTaskId(), entity.getPersonId(), entity.getRoleCode());
    }

    private MeetingEntity toMeetingEntity(CollaborationRecords.Meeting meeting) {
        MeetingEntity entity = new MeetingEntity();
        entity.setMeetingId(meeting.meetingId());
        entity.setTenantId(meeting.tenantId());
        entity.setWorkspaceId(meeting.workspaceId());
        entity.setTitle(meeting.title());
        entity.setAgenda(meeting.agenda());
        entity.setOrganizerId(meeting.organizerId());
        entity.setStatus(meeting.status());
        entity.setStartAt(meeting.startAt());
        entity.setEndAt(meeting.endAt());
        entity.setLocation(meeting.location());
        entity.setReminderMinutesBefore(meeting.reminderMinutesBefore());
        entity.setMinutes(meeting.minutes());
        entity.setMinutesPublishedAt(meeting.minutesPublishedAt());
        entity.setCreatedAt(meeting.createdAt());
        entity.setUpdatedAt(meeting.updatedAt());
        return entity;
    }

    private CollaborationRecords.Meeting toMeeting(MeetingEntity entity) {
        return new CollaborationRecords.Meeting(entity.getMeetingId(), entity.getTenantId(), entity.getWorkspaceId(),
                entity.getTitle(), entity.getAgenda(), entity.getOrganizerId(), entity.getStatus(),
                entity.getStartAt(), entity.getEndAt(), entity.getLocation(), entity.getReminderMinutesBefore(),
                entity.getMinutes(), entity.getMinutesPublishedAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private MeetingParticipantEntity toMeetingParticipantEntity(
            CollaborationRecords.MeetingParticipant participant
    ) {
        MeetingParticipantEntity entity = new MeetingParticipantEntity();
        entity.setParticipantId(participant.participantId());
        entity.setTenantId(participant.tenantId());
        entity.setMeetingId(participant.meetingId());
        entity.setPersonId(participant.personId());
        entity.setRoleCode(participant.roleCode());
        entity.setResponseStatus(participant.responseStatus());
        return entity;
    }

    private CollaborationRecords.MeetingParticipant toMeetingParticipant(MeetingParticipantEntity entity) {
        return new CollaborationRecords.MeetingParticipant(entity.getParticipantId(), entity.getTenantId(),
                entity.getMeetingId(), entity.getPersonId(), entity.getRoleCode(), entity.getResponseStatus());
    }

    private MeetingReminderEntity toMeetingReminderEntity(CollaborationRecords.MeetingReminder reminder) {
        MeetingReminderEntity entity = new MeetingReminderEntity();
        entity.setReminderId(reminder.reminderId());
        entity.setTenantId(reminder.tenantId());
        entity.setMeetingId(reminder.meetingId());
        entity.setParticipantId(reminder.participantId());
        entity.setRemindAt(reminder.remindAt());
        entity.setStatus(reminder.status());
        entity.setSentAt(reminder.sentAt());
        return entity;
    }

    private CollaborationRecords.MeetingReminder toMeetingReminder(MeetingReminderEntity entity) {
        return new CollaborationRecords.MeetingReminder(entity.getReminderId(), entity.getTenantId(),
                entity.getMeetingId(), entity.getParticipantId(), entity.getRemindAt(), entity.getStatus(),
                entity.getSentAt());
    }

    private AttachmentLinkEntity toAttachmentLinkEntity(CollaborationRecords.AttachmentLink attachmentLink) {
        AttachmentLinkEntity entity = new AttachmentLinkEntity();
        entity.setAttachmentLinkId(attachmentLink.attachmentLinkId());
        entity.setTenantId(attachmentLink.tenantId());
        entity.setOwnerType(attachmentLink.ownerType());
        entity.setOwnerId(attachmentLink.ownerId());
        entity.setAttachmentId(attachmentLink.attachmentId());
        entity.setFileName(attachmentLink.fileName());
        entity.setCreatedAt(attachmentLink.createdAt());
        return entity;
    }

    private CollaborationRecords.AttachmentLink toAttachmentLink(AttachmentLinkEntity entity) {
        return new CollaborationRecords.AttachmentLink(entity.getAttachmentLinkId(), entity.getTenantId(),
                entity.getOwnerType(), entity.getOwnerId(), entity.getAttachmentId(), entity.getFileName(),
                entity.getCreatedAt());
    }

    private AuditRecordEntity toAuditRecordEntity(CollaborationRecords.AuditRecord auditRecord) {
        AuditRecordEntity entity = new AuditRecordEntity();
        entity.setAuditId(auditRecord.auditId());
        entity.setTenantId(auditRecord.tenantId());
        entity.setActorId(auditRecord.actorId());
        entity.setActionCode(auditRecord.actionCode());
        entity.setResourceType(auditRecord.resourceType());
        entity.setResourceId(auditRecord.resourceId());
        entity.setRequestId(auditRecord.requestId());
        entity.setDetail(auditRecord.detail());
        entity.setOccurredAt(auditRecord.occurredAt());
        return entity;
    }

    private CollaborationRecords.AuditRecord toAuditRecord(AuditRecordEntity entity) {
        return new CollaborationRecords.AuditRecord(entity.getAuditId(), entity.getTenantId(), entity.getActorId(),
                entity.getActionCode(), entity.getResourceType(), entity.getResourceId(), entity.getRequestId(),
                entity.getDetail(), entity.getOccurredAt());
    }

    private IdempotencyRecordEntity toIdempotencyRecordEntity(
            CollaborationRecords.IdempotencyRecord idempotencyRecord
    ) {
        IdempotencyRecordEntity entity = new IdempotencyRecordEntity();
        entity.setIdempotencyId(idempotencyRecord.idempotencyId());
        entity.setTenantId(idempotencyRecord.tenantId());
        entity.setOperationCode(idempotencyRecord.operationCode());
        entity.setIdempotencyKey(idempotencyRecord.idempotencyKey());
        entity.setResourceType(idempotencyRecord.resourceType());
        entity.setResourceId(idempotencyRecord.resourceId());
        entity.setCreatedAt(idempotencyRecord.createdAt());
        return entity;
    }

    private CollaborationRecords.IdempotencyRecord toIdempotencyRecord(IdempotencyRecordEntity entity) {
        return new CollaborationRecords.IdempotencyRecord(entity.getIdempotencyId(), entity.getTenantId(),
                entity.getOperationCode(), entity.getIdempotencyKey(), entity.getResourceType(),
                entity.getResourceId(), entity.getCreatedAt());
    }
}
