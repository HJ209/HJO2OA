package com.hjo2oa.biz.collaboration.hub.infrastructure;

import com.hjo2oa.biz.collaboration.hub.domain.CollaborationConstants;
import com.hjo2oa.biz.collaboration.hub.domain.CollaborationRecords;
import com.hjo2oa.biz.collaboration.hub.domain.CollaborationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCollaborationRepository implements CollaborationRepository {

    private final Map<String, CollaborationRecords.Workspace> workspaces = new ConcurrentHashMap<>();
    private final Map<String, CollaborationRecords.WorkspaceMember> members = new ConcurrentHashMap<>();
    private final Map<String, CollaborationRecords.Discussion> discussions = new ConcurrentHashMap<>();
    private final Map<String, CollaborationRecords.DiscussionReply> replies = new ConcurrentHashMap<>();
    private final Map<String, CollaborationRecords.DiscussionRead> reads = new ConcurrentHashMap<>();
    private final Map<String, CollaborationRecords.Comment> comments = new ConcurrentHashMap<>();
    private final Map<String, CollaborationRecords.Task> tasks = new ConcurrentHashMap<>();
    private final Map<String, CollaborationRecords.TaskParticipant> taskParticipants = new ConcurrentHashMap<>();
    private final Map<String, CollaborationRecords.Meeting> meetings = new ConcurrentHashMap<>();
    private final Map<String, CollaborationRecords.MeetingParticipant> meetingParticipants = new ConcurrentHashMap<>();
    private final Map<String, CollaborationRecords.MeetingReminder> meetingReminders = new ConcurrentHashMap<>();
    private final Map<String, CollaborationRecords.AttachmentLink> attachmentLinks = new ConcurrentHashMap<>();
    private final Map<String, CollaborationRecords.AuditRecord> auditRecords = new ConcurrentHashMap<>();
    private final Map<String, CollaborationRecords.IdempotencyRecord> idempotencyRecords = new ConcurrentHashMap<>();

    @Override
    public CollaborationRecords.Workspace saveWorkspace(CollaborationRecords.Workspace workspace) {
        workspaces.put(workspace.workspaceId(), workspace);
        return workspace;
    }

    @Override
    public Optional<CollaborationRecords.Workspace> findWorkspace(String tenantId, String workspaceId) {
        return Optional.ofNullable(workspaces.get(workspaceId))
                .filter(workspace -> workspace.tenantId().equals(tenantId));
    }

    @Override
    public Optional<CollaborationRecords.Workspace> findWorkspaceByCode(String tenantId, String code) {
        return workspaces.values().stream()
                .filter(workspace -> workspace.tenantId().equals(tenantId))
                .filter(workspace -> workspace.code().equals(code))
                .findFirst();
    }

    @Override
    public List<CollaborationRecords.Workspace> listWorkspaces(String tenantId) {
        return workspaces.values().stream()
                .filter(workspace -> workspace.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public CollaborationRecords.WorkspaceMember saveMember(CollaborationRecords.WorkspaceMember member) {
        findMember(member.tenantId(), member.workspaceId(), member.personId())
                .ifPresent(existing -> members.remove(existing.memberId()));
        members.put(member.memberId(), member);
        return member;
    }

    @Override
    public void deleteMembersByWorkspace(String tenantId, String workspaceId) {
        listMembers(tenantId, workspaceId).forEach(member -> members.remove(member.memberId()));
    }

    @Override
    public List<CollaborationRecords.WorkspaceMember> listMembers(String tenantId, String workspaceId) {
        return members.values().stream()
                .filter(member -> member.tenantId().equals(tenantId))
                .filter(member -> member.workspaceId().equals(workspaceId))
                .toList();
    }

    @Override
    public Optional<CollaborationRecords.WorkspaceMember> findMember(
            String tenantId,
            String workspaceId,
            String personId
    ) {
        return members.values().stream()
                .filter(member -> member.tenantId().equals(tenantId))
                .filter(member -> member.workspaceId().equals(workspaceId))
                .filter(member -> member.personId().equals(personId))
                .findFirst();
    }

    @Override
    public List<CollaborationRecords.WorkspaceMember> listMembershipsByPerson(String tenantId, String personId) {
        return members.values().stream()
                .filter(member -> member.tenantId().equals(tenantId))
                .filter(member -> member.personId().equals(personId))
                .toList();
    }

    @Override
    public CollaborationRecords.Discussion saveDiscussion(CollaborationRecords.Discussion discussion) {
        discussions.put(discussion.discussionId(), discussion);
        return discussion;
    }

    @Override
    public Optional<CollaborationRecords.Discussion> findDiscussion(String tenantId, String discussionId) {
        return Optional.ofNullable(discussions.get(discussionId))
                .filter(discussion -> discussion.tenantId().equals(tenantId));
    }

    @Override
    public List<CollaborationRecords.Discussion> listDiscussions(String tenantId, String workspaceId) {
        return discussions.values().stream()
                .filter(discussion -> discussion.tenantId().equals(tenantId))
                .filter(discussion -> discussion.workspaceId().equals(workspaceId))
                .toList();
    }

    @Override
    public CollaborationRecords.DiscussionReply saveReply(CollaborationRecords.DiscussionReply reply) {
        replies.put(reply.replyId(), reply);
        return reply;
    }

    @Override
    public List<CollaborationRecords.DiscussionReply> listReplies(String tenantId, String discussionId) {
        return replies.values().stream()
                .filter(reply -> reply.tenantId().equals(tenantId))
                .filter(reply -> reply.discussionId().equals(discussionId))
                .sorted(Comparator.comparing(CollaborationRecords.DiscussionReply::createdAt))
                .toList();
    }

    @Override
    public CollaborationRecords.DiscussionRead saveRead(CollaborationRecords.DiscussionRead read) {
        findRead(read.tenantId(), read.discussionId(), read.personId())
                .ifPresent(existing -> reads.remove(existing.readId()));
        reads.put(read.readId(), read);
        return read;
    }

    @Override
    public Optional<CollaborationRecords.DiscussionRead> findRead(
            String tenantId,
            String discussionId,
            String personId
    ) {
        return reads.values().stream()
                .filter(read -> read.tenantId().equals(tenantId))
                .filter(read -> read.discussionId().equals(discussionId))
                .filter(read -> read.personId().equals(personId))
                .findFirst();
    }

    @Override
    public CollaborationRecords.Comment saveComment(CollaborationRecords.Comment comment) {
        comments.put(comment.commentId(), comment);
        return comment;
    }

    @Override
    public Optional<CollaborationRecords.Comment> findComment(String tenantId, String commentId) {
        return Optional.ofNullable(comments.get(commentId))
                .filter(comment -> comment.tenantId().equals(tenantId));
    }

    @Override
    public List<CollaborationRecords.Comment> listComments(String tenantId, String objectType, String objectId) {
        return comments.values().stream()
                .filter(comment -> comment.tenantId().equals(tenantId))
                .filter(comment -> comment.objectType().equals(objectType))
                .filter(comment -> comment.objectId().equals(objectId))
                .sorted(Comparator.comparing(CollaborationRecords.Comment::createdAt))
                .toList();
    }

    @Override
    public CollaborationRecords.Task saveTask(CollaborationRecords.Task task) {
        tasks.put(task.taskId(), task);
        return task;
    }

    @Override
    public Optional<CollaborationRecords.Task> findTask(String tenantId, String taskId) {
        return Optional.ofNullable(tasks.get(taskId))
                .filter(task -> task.tenantId().equals(tenantId));
    }

    @Override
    public List<CollaborationRecords.Task> listTasks(String tenantId) {
        return tasks.values().stream()
                .filter(task -> task.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public CollaborationRecords.TaskParticipant saveTaskParticipant(
            CollaborationRecords.TaskParticipant participant
    ) {
        taskParticipants.put(participant.participantId(), participant);
        return participant;
    }

    @Override
    public void deleteTaskParticipants(String tenantId, String taskId) {
        listTaskParticipants(tenantId, taskId)
                .forEach(participant -> taskParticipants.remove(participant.participantId()));
    }

    @Override
    public List<CollaborationRecords.TaskParticipant> listTaskParticipants(String tenantId, String taskId) {
        return taskParticipants.values().stream()
                .filter(participant -> participant.tenantId().equals(tenantId))
                .filter(participant -> participant.taskId().equals(taskId))
                .toList();
    }

    @Override
    public CollaborationRecords.Meeting saveMeeting(CollaborationRecords.Meeting meeting) {
        meetings.put(meeting.meetingId(), meeting);
        return meeting;
    }

    @Override
    public Optional<CollaborationRecords.Meeting> findMeeting(String tenantId, String meetingId) {
        return Optional.ofNullable(meetings.get(meetingId))
                .filter(meeting -> meeting.tenantId().equals(tenantId));
    }

    @Override
    public List<CollaborationRecords.Meeting> listMeetings(String tenantId) {
        return meetings.values().stream()
                .filter(meeting -> meeting.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public CollaborationRecords.MeetingParticipant saveMeetingParticipant(
            CollaborationRecords.MeetingParticipant participant
    ) {
        meetingParticipants.put(participant.participantId(), participant);
        return participant;
    }

    @Override
    public void deleteMeetingParticipants(String tenantId, String meetingId) {
        listMeetingParticipants(tenantId, meetingId)
                .forEach(participant -> meetingParticipants.remove(participant.participantId()));
    }

    @Override
    public List<CollaborationRecords.MeetingParticipant> listMeetingParticipants(String tenantId, String meetingId) {
        return meetingParticipants.values().stream()
                .filter(participant -> participant.tenantId().equals(tenantId))
                .filter(participant -> participant.meetingId().equals(meetingId))
                .toList();
    }

    @Override
    public CollaborationRecords.MeetingReminder saveMeetingReminder(CollaborationRecords.MeetingReminder reminder) {
        meetingReminders.put(reminder.reminderId(), reminder);
        return reminder;
    }

    @Override
    public List<CollaborationRecords.MeetingReminder> listDueMeetingReminders(String tenantId, Instant dueAt) {
        return meetingReminders.values().stream()
                .filter(reminder -> reminder.tenantId().equals(tenantId))
                .filter(reminder -> CollaborationConstants.STATUS_PENDING.equals(reminder.status()))
                .filter(reminder -> !reminder.remindAt().isAfter(dueAt))
                .toList();
    }

    @Override
    public List<CollaborationRecords.MeetingReminder> listMeetingReminders(String tenantId, String meetingId) {
        return meetingReminders.values().stream()
                .filter(reminder -> reminder.tenantId().equals(tenantId))
                .filter(reminder -> reminder.meetingId().equals(meetingId))
                .toList();
    }

    @Override
    public CollaborationRecords.AttachmentLink saveAttachmentLink(CollaborationRecords.AttachmentLink attachmentLink) {
        attachmentLinks.put(attachmentLink.attachmentLinkId(), attachmentLink);
        return attachmentLink;
    }

    @Override
    public List<CollaborationRecords.AttachmentLink> listAttachmentLinks(
            String tenantId,
            String ownerType,
            String ownerId
    ) {
        return attachmentLinks.values().stream()
                .filter(attachmentLink -> attachmentLink.tenantId().equals(tenantId))
                .filter(attachmentLink -> attachmentLink.ownerType().equals(ownerType))
                .filter(attachmentLink -> attachmentLink.ownerId().equals(ownerId))
                .toList();
    }

    @Override
    public CollaborationRecords.AuditRecord saveAudit(CollaborationRecords.AuditRecord auditRecord) {
        auditRecords.put(auditRecord.auditId(), auditRecord);
        return auditRecord;
    }

    @Override
    public List<CollaborationRecords.AuditRecord> listAuditRecords(String tenantId, int limit) {
        return auditRecords.values().stream()
                .filter(auditRecord -> auditRecord.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(CollaborationRecords.AuditRecord::occurredAt).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public Optional<CollaborationRecords.IdempotencyRecord> findIdempotency(
            String tenantId,
            String operationCode,
            String idempotencyKey
    ) {
        return idempotencyRecords.values().stream()
                .filter(record -> record.tenantId().equals(tenantId))
                .filter(record -> record.operationCode().equals(operationCode))
                .filter(record -> record.idempotencyKey().equals(idempotencyKey))
                .findFirst();
    }

    @Override
    public CollaborationRecords.IdempotencyRecord saveIdempotency(
            CollaborationRecords.IdempotencyRecord idempotencyRecord
    ) {
        idempotencyRecords.put(idempotencyRecord.idempotencyId(), idempotencyRecord);
        return idempotencyRecord;
    }

    public void clear() {
        List.of(
                new ArrayList<>(workspaces.keySet()),
                new ArrayList<>(members.keySet()),
                new ArrayList<>(discussions.keySet()),
                new ArrayList<>(replies.keySet()),
                new ArrayList<>(reads.keySet()),
                new ArrayList<>(comments.keySet()),
                new ArrayList<>(tasks.keySet()),
                new ArrayList<>(taskParticipants.keySet()),
                new ArrayList<>(meetings.keySet()),
                new ArrayList<>(meetingParticipants.keySet()),
                new ArrayList<>(meetingReminders.keySet()),
                new ArrayList<>(attachmentLinks.keySet()),
                new ArrayList<>(auditRecords.keySet()),
                new ArrayList<>(idempotencyRecords.keySet())
        );
        workspaces.clear();
        members.clear();
        discussions.clear();
        replies.clear();
        reads.clear();
        comments.clear();
        tasks.clear();
        taskParticipants.clear();
        meetings.clear();
        meetingParticipants.clear();
        meetingReminders.clear();
        attachmentLinks.clear();
        auditRecords.clear();
        idempotencyRecords.clear();
    }
}
