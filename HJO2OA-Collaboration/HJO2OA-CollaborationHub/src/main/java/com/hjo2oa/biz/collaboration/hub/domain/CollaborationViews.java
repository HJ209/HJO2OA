package com.hjo2oa.biz.collaboration.hub.domain;

import java.time.Instant;
import java.util.List;

public final class CollaborationViews {

    private CollaborationViews() {
    }

    public record AttachmentView(String attachmentId, String fileName) {
    }

    public record MemberView(String personId, String roleCode, List<String> permissions, String status) {
    }

    public record WorkspaceView(
            String workspaceId,
            String code,
            String name,
            String description,
            String status,
            String visibility,
            String ownerId,
            List<MemberView> members,
            Instant updatedAt
    ) {
    }

    public record DiscussionReplyView(
            String replyId,
            String authorId,
            String body,
            boolean deleted,
            Instant createdAt
    ) {
    }

    public record DiscussionView(
            String discussionId,
            String workspaceId,
            String title,
            String body,
            String authorId,
            String status,
            boolean pinned,
            boolean readByCurrentUser,
            List<AttachmentView> attachments,
            List<DiscussionReplyView> replies,
            Instant updatedAt
    ) {
    }

    public record CommentView(
            String commentId,
            String workspaceId,
            String objectType,
            String objectId,
            String authorId,
            String body,
            boolean deleted,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record TaskView(
            String taskId,
            String workspaceId,
            String title,
            String description,
            String creatorId,
            String assigneeId,
            String status,
            String priority,
            Instant dueAt,
            Instant completedAt,
            List<String> participantIds,
            List<CommentView> comments,
            Instant updatedAt
    ) {
    }

    public record MeetingView(
            String meetingId,
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
            List<String> participantIds,
            Instant updatedAt
    ) {
    }

    public record AuditView(
            String auditId,
            String actorId,
            String actionCode,
            String resourceType,
            String resourceId,
            String requestId,
            String detail,
            Instant occurredAt
    ) {
    }

    public record CollaborationSnapshot(
            List<WorkspaceView> workspaces,
            List<DiscussionView> discussions,
            List<TaskView> tasks,
            List<MeetingView> meetings,
            List<AuditView> auditTrail
    ) {
    }

    public record ReminderTriggerResult(int scannedCount, int sentCount) {
    }
}
