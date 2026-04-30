package com.hjo2oa.biz.collaboration.hub.interfaces;

import com.hjo2oa.biz.collaboration.hub.application.CollaborationCommands;
import java.time.Instant;
import java.util.List;

public final class CollaborationDtos {

    private CollaborationDtos() {
    }

    public record AttachmentRequest(String attachmentId, String fileName) {

        CollaborationCommands.AttachmentCommand toCommand() {
            return new CollaborationCommands.AttachmentCommand(attachmentId, fileName);
        }
    }

    public record MemberRequest(String personId, String roleCode, List<String> permissions) {

        CollaborationCommands.MemberCommand toCommand() {
            return new CollaborationCommands.MemberCommand(personId, roleCode, permissions);
        }
    }

    public record CreateWorkspaceRequest(
            String code,
            String name,
            String description,
            String visibility,
            List<MemberRequest> members
    ) {

        CollaborationCommands.CreateWorkspaceCommand toCommand() {
            return new CollaborationCommands.CreateWorkspaceCommand(
                    code,
                    name,
                    description,
                    visibility,
                    mapMembers(members)
            );
        }
    }

    public record ReplaceMembersRequest(List<MemberRequest> members) {

        CollaborationCommands.ReplaceMembersCommand toCommand() {
            return new CollaborationCommands.ReplaceMembersCommand(mapMembers(members));
        }
    }

    public record CreateDiscussionRequest(
            String title,
            String body,
            List<String> mentionPersonIds,
            List<AttachmentRequest> attachments
    ) {

        CollaborationCommands.CreateDiscussionCommand toCommand() {
            return new CollaborationCommands.CreateDiscussionCommand(
                    title,
                    body,
                    mentionPersonIds,
                    mapAttachments(attachments)
            );
        }
    }

    public record ReplyDiscussionRequest(
            String body,
            List<String> mentionPersonIds,
            List<AttachmentRequest> attachments
    ) {

        CollaborationCommands.ReplyDiscussionCommand toCommand() {
            return new CollaborationCommands.ReplyDiscussionCommand(body, mentionPersonIds, mapAttachments(attachments));
        }
    }

    public record CreateCommentRequest(
            String workspaceId,
            String objectType,
            String objectId,
            String body,
            List<String> mentionPersonIds,
            List<AttachmentRequest> attachments
    ) {

        CollaborationCommands.CreateCommentCommand toCommand() {
            return new CollaborationCommands.CreateCommentCommand(
                    workspaceId,
                    objectType,
                    objectId,
                    body,
                    mentionPersonIds,
                    mapAttachments(attachments)
            );
        }
    }

    public record CreateTaskRequest(
            String workspaceId,
            String title,
            String description,
            String assigneeId,
            List<String> participantIds,
            String priority,
            Instant dueAt
    ) {

        CollaborationCommands.CreateTaskCommand toCommand() {
            return new CollaborationCommands.CreateTaskCommand(
                    workspaceId,
                    title,
                    description,
                    assigneeId,
                    participantIds,
                    priority,
                    dueAt
            );
        }
    }

    public record ChangeTaskStatusRequest(String status, String assigneeId, String comment) {

        CollaborationCommands.ChangeTaskStatusCommand toCommand() {
            return new CollaborationCommands.ChangeTaskStatusCommand(status, assigneeId, comment);
        }
    }

    public record CreateMeetingRequest(
            String workspaceId,
            String title,
            String agenda,
            Instant startAt,
            Instant endAt,
            String location,
            int reminderMinutesBefore,
            List<String> participantIds
    ) {

        CollaborationCommands.CreateMeetingCommand toCommand() {
            return new CollaborationCommands.CreateMeetingCommand(
                    workspaceId,
                    title,
                    agenda,
                    startAt,
                    endAt,
                    location,
                    reminderMinutesBefore,
                    participantIds
            );
        }
    }

    public record ChangeMeetingStatusRequest(String status, String reason) {

        CollaborationCommands.ChangeMeetingStatusCommand toCommand() {
            return new CollaborationCommands.ChangeMeetingStatusCommand(status, reason);
        }
    }

    public record PublishMeetingMinutesRequest(String minutes, List<String> mentionPersonIds) {

        CollaborationCommands.PublishMeetingMinutesCommand toCommand() {
            return new CollaborationCommands.PublishMeetingMinutesCommand(minutes, mentionPersonIds);
        }
    }

    public record TriggerReminderRequest(Instant dueAt) {
    }

    private static List<CollaborationCommands.MemberCommand> mapMembers(List<MemberRequest> members) {
        if (members == null) {
            return List.of();
        }
        return members.stream().map(MemberRequest::toCommand).toList();
    }

    private static List<CollaborationCommands.AttachmentCommand> mapAttachments(List<AttachmentRequest> attachments) {
        if (attachments == null) {
            return List.of();
        }
        return attachments.stream().map(AttachmentRequest::toCommand).toList();
    }
}
