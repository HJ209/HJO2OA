package com.hjo2oa.biz.collaboration.hub.domain;

import java.time.Instant;

public final class CollaborationRecords {

    private CollaborationRecords() {
    }

    public record Workspace(
            String workspaceId,
            String tenantId,
            String code,
            String name,
            String description,
            String status,
            String visibility,
            String ownerId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record WorkspaceMember(
            String memberId,
            String tenantId,
            String workspaceId,
            String personId,
            String roleCode,
            String permissions,
            String status,
            Instant joinedAt,
            Instant updatedAt
    ) {
    }

    public record Discussion(
            String discussionId,
            String tenantId,
            String workspaceId,
            String title,
            String body,
            String authorId,
            String status,
            boolean pinned,
            Instant createdAt,
            Instant updatedAt,
            Instant closedAt
    ) {
    }

    public record DiscussionReply(
            String replyId,
            String tenantId,
            String discussionId,
            String authorId,
            String body,
            boolean deleted,
            Instant createdAt,
            Instant deletedAt
    ) {
    }

    public record DiscussionRead(
            String readId,
            String tenantId,
            String discussionId,
            String personId,
            Instant readAt
    ) {
    }

    public record Comment(
            String commentId,
            String tenantId,
            String workspaceId,
            String objectType,
            String objectId,
            String authorId,
            String body,
            boolean deleted,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
    }

    public record Task(
            String taskId,
            String tenantId,
            String workspaceId,
            String title,
            String description,
            String creatorId,
            String assigneeId,
            String status,
            String priority,
            Instant dueAt,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record TaskParticipant(
            String participantId,
            String tenantId,
            String taskId,
            String personId,
            String roleCode
    ) {
    }

    public record Meeting(
            String meetingId,
            String tenantId,
            String workspaceId,
            String title,
            String agenda,
            String organizerId,
            String status,
            Instant startAt,
            Instant endAt,
            String location,
            int reminderMinutesBefore,
            String minutes,
            Instant minutesPublishedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record MeetingParticipant(
            String participantId,
            String tenantId,
            String meetingId,
            String personId,
            String roleCode,
            String responseStatus
    ) {
    }

    public record MeetingReminder(
            String reminderId,
            String tenantId,
            String meetingId,
            String participantId,
            Instant remindAt,
            String status,
            Instant sentAt
    ) {
    }

    public record AttachmentLink(
            String attachmentLinkId,
            String tenantId,
            String ownerType,
            String ownerId,
            String attachmentId,
            String fileName,
            Instant createdAt
    ) {
    }

    public record AuditRecord(
            String auditId,
            String tenantId,
            String actorId,
            String actionCode,
            String resourceType,
            String resourceId,
            String requestId,
            String detail,
            Instant occurredAt
    ) {
    }

    public record IdempotencyRecord(
            String idempotencyId,
            String tenantId,
            String operationCode,
            String idempotencyKey,
            String resourceType,
            String resourceId,
            Instant createdAt
    ) {
    }
}
