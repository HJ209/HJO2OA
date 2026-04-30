package com.hjo2oa.msg.message.center.application;

import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationPriority;
import java.time.Instant;

public final class MessageNotificationCommands {

    private MessageNotificationCommands() {
    }

    public record CreateNotificationCommand(
            String dedupKey,
            String tenantId,
            String recipientId,
            String targetAssignmentId,
            String targetPositionId,
            String title,
            String bodySummary,
            String deepLink,
            NotificationCategory category,
            NotificationPriority priority,
            String sourceModule,
            String sourceEventType,
            String sourceBusinessId,
            Instant occurredAt
    ) {
    }
}
