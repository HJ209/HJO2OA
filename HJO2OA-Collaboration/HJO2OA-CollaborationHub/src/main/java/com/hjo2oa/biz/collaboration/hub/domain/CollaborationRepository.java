package com.hjo2oa.biz.collaboration.hub.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CollaborationRepository {

    CollaborationRecords.Workspace saveWorkspace(CollaborationRecords.Workspace workspace);

    Optional<CollaborationRecords.Workspace> findWorkspace(String tenantId, String workspaceId);

    Optional<CollaborationRecords.Workspace> findWorkspaceByCode(String tenantId, String code);

    List<CollaborationRecords.Workspace> listWorkspaces(String tenantId);

    CollaborationRecords.WorkspaceMember saveMember(CollaborationRecords.WorkspaceMember member);

    void deleteMembersByWorkspace(String tenantId, String workspaceId);

    List<CollaborationRecords.WorkspaceMember> listMembers(String tenantId, String workspaceId);

    Optional<CollaborationRecords.WorkspaceMember> findMember(String tenantId, String workspaceId, String personId);

    List<CollaborationRecords.WorkspaceMember> listMembershipsByPerson(String tenantId, String personId);

    CollaborationRecords.Discussion saveDiscussion(CollaborationRecords.Discussion discussion);

    Optional<CollaborationRecords.Discussion> findDiscussion(String tenantId, String discussionId);

    List<CollaborationRecords.Discussion> listDiscussions(String tenantId, String workspaceId);

    CollaborationRecords.DiscussionReply saveReply(CollaborationRecords.DiscussionReply reply);

    List<CollaborationRecords.DiscussionReply> listReplies(String tenantId, String discussionId);

    CollaborationRecords.DiscussionRead saveRead(CollaborationRecords.DiscussionRead read);

    Optional<CollaborationRecords.DiscussionRead> findRead(String tenantId, String discussionId, String personId);

    CollaborationRecords.Comment saveComment(CollaborationRecords.Comment comment);

    Optional<CollaborationRecords.Comment> findComment(String tenantId, String commentId);

    List<CollaborationRecords.Comment> listComments(String tenantId, String objectType, String objectId);

    CollaborationRecords.Task saveTask(CollaborationRecords.Task task);

    Optional<CollaborationRecords.Task> findTask(String tenantId, String taskId);

    List<CollaborationRecords.Task> listTasks(String tenantId);

    CollaborationRecords.TaskParticipant saveTaskParticipant(CollaborationRecords.TaskParticipant participant);

    void deleteTaskParticipants(String tenantId, String taskId);

    List<CollaborationRecords.TaskParticipant> listTaskParticipants(String tenantId, String taskId);

    CollaborationRecords.Meeting saveMeeting(CollaborationRecords.Meeting meeting);

    Optional<CollaborationRecords.Meeting> findMeeting(String tenantId, String meetingId);

    List<CollaborationRecords.Meeting> listMeetings(String tenantId);

    CollaborationRecords.MeetingParticipant saveMeetingParticipant(
            CollaborationRecords.MeetingParticipant participant
    );

    void deleteMeetingParticipants(String tenantId, String meetingId);

    List<CollaborationRecords.MeetingParticipant> listMeetingParticipants(String tenantId, String meetingId);

    CollaborationRecords.MeetingReminder saveMeetingReminder(CollaborationRecords.MeetingReminder reminder);

    List<CollaborationRecords.MeetingReminder> listDueMeetingReminders(String tenantId, Instant dueAt);

    List<CollaborationRecords.MeetingReminder> listMeetingReminders(String tenantId, String meetingId);

    CollaborationRecords.AttachmentLink saveAttachmentLink(CollaborationRecords.AttachmentLink attachmentLink);

    List<CollaborationRecords.AttachmentLink> listAttachmentLinks(String tenantId, String ownerType, String ownerId);

    CollaborationRecords.AuditRecord saveAudit(CollaborationRecords.AuditRecord auditRecord);

    List<CollaborationRecords.AuditRecord> listAuditRecords(String tenantId, int limit);

    Optional<CollaborationRecords.IdempotencyRecord> findIdempotency(
            String tenantId,
            String operationCode,
            String idempotencyKey
    );

    CollaborationRecords.IdempotencyRecord saveIdempotency(
            CollaborationRecords.IdempotencyRecord idempotencyRecord
    );
}
