package com.hjo2oa.biz.collaboration.hub.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public final class CollaborationEvents {

    private CollaborationEvents() {
    }

    public record MentionCreatedEvent(
            UUID eventId,
            Instant occurredAt,
            String tenantId,
            String recipientId,
            String actorId,
            String workspaceId,
            String sourceType,
            String sourceId,
            String title,
            String deepLink
    ) implements DomainEvent {

        @Override
        public String eventType() {
            return "biz.collaboration.mentioned";
        }
    }

    public record TaskAssignedEvent(
            UUID eventId,
            Instant occurredAt,
            String tenantId,
            String taskId,
            String assigneeId,
            String title,
            String priority,
            Instant dueAt,
            String deepLink
    ) implements DomainEvent {

        @Override
        public String eventType() {
            return "biz.task.assigned";
        }
    }

    public record TaskChangedEvent(
            UUID eventId,
            Instant occurredAt,
            String tenantId,
            String taskId,
            String recipientId,
            String title,
            String oldStatus,
            String newStatus,
            String deepLink
    ) implements DomainEvent {

        @Override
        public String eventType() {
            return CollaborationConstants.STATUS_DONE.equals(newStatus)
                    ? "biz.task.completed"
                    : "biz.task.updated";
        }
    }

    public record MeetingReminderDueEvent(
            UUID eventId,
            Instant occurredAt,
            String tenantId,
            String meetingId,
            String participantId,
            String title,
            Instant startAt,
            String deepLink
    ) implements DomainEvent {

        @Override
        public String eventType() {
            return "biz.meeting.reminder-due";
        }
    }
}
