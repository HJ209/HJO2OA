package com.hjo2oa.msg.message.center.domain;

import java.time.Instant;

public record NotificationSummary(
        String notificationId,
        String title,
        String bodySummary,
        NotificationCategory category,
        NotificationPriority priority,
        NotificationInboxStatus inboxStatus,
        NotificationDeliveryStatus deliveryStatus,
        String sourceModule,
        String deepLink,
        String targetAssignmentId,
        String targetPositionId,
        Instant createdAt
) {
}
