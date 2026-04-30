package com.hjo2oa.biz.collaboration.hub.application;

import java.time.Instant;
import java.util.List;

public final class CollaborationCommands {

    private CollaborationCommands() {
    }

    public record AttachmentCommand(String attachmentId, String fileName) {
    }

    public record MemberCommand(String personId, String roleCode, List<String> permissions) {
    }

    public record CreateWorkspaceCommand(
            String code,
            String name,
            String description,
            String visibility,
            List<MemberCommand> members
    ) {
    }

    public record ReplaceMembersCommand(List<MemberCommand> members) {
    }

    public record CreateDiscussionCommand(
            String title,
            String body,
            List<String> mentionPersonIds,
            List<AttachmentCommand> attachments
    ) {
    }

    public record ReplyDiscussionCommand(
            String body,
            List<String> mentionPersonIds,
            List<AttachmentCommand> attachments
    ) {
    }

    public record CreateCommentCommand(
            String workspaceId,
            String objectType,
            String objectId,
            String body,
            List<String> mentionPersonIds,
            List<AttachmentCommand> attachments
    ) {
    }

    public record CreateTaskCommand(
            String workspaceId,
            String title,
            String description,
            String assigneeId,
            List<String> participantIds,
            String priority,
            Instant dueAt
    ) {
    }

    public record ChangeTaskStatusCommand(String status, String assigneeId, String comment) {
    }

    public record CreateMeetingCommand(
            String workspaceId,
            String title,
            String agenda,
            Instant startAt,
            Instant endAt,
            String location,
            int reminderMinutesBefore,
            List<String> participantIds
    ) {
    }

    public record ChangeMeetingStatusCommand(String status, String reason) {
    }

    public record PublishMeetingMinutesCommand(String minutes, List<String> mentionPersonIds) {
    }
}
