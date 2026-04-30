package com.hjo2oa.msg.message.center.interfaces;

import com.hjo2oa.msg.message.center.application.MessageNotificationCommands;
import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record NotificationCreateRequest(
        @NotBlank @Size(max = 255) String dedupKey,
        @NotBlank @Size(max = 128) String tenantId,
        @NotBlank @Size(max = 128) String recipientId,
        @Size(max = 128) String targetAssignmentId,
        @Size(max = 128) String targetPositionId,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 1024) String bodySummary,
        @NotBlank @Size(max = 512) String deepLink,
        @NotNull NotificationCategory category,
        @NotNull NotificationPriority priority,
        @NotBlank @Size(max = 128) String sourceModule,
        @NotBlank @Size(max = 255) String sourceEventType,
        @NotBlank @Size(max = 128) String sourceBusinessId,
        Instant occurredAt
) {

    public MessageNotificationCommands.CreateNotificationCommand toCommand() {
        return new MessageNotificationCommands.CreateNotificationCommand(
                dedupKey,
                tenantId,
                recipientId,
                targetAssignmentId,
                targetPositionId,
                title,
                bodySummary,
                deepLink,
                category,
                priority,
                sourceModule,
                sourceEventType,
                sourceBusinessId,
                occurredAt
        );
    }
}
