package com.hjo2oa.msg.message.center.domain;

import java.time.Instant;

public record NotificationDetail(
        String notificationId,
        String title,
        String bodySummary,
        NotificationCategory category,
        NotificationPriority priority,
        NotificationInboxStatus inboxStatus,
        NotificationDeliveryStatus deliveryStatus,
        String sourceModule,
        String sourceEventType,
        String sourceBusinessId,
        String deepLink,
        String targetAssignmentId,
        String targetPositionId,
        Instant createdAt,
        Instant readAt,
        Instant archivedAt,
        Instant revokedAt,
        Instant expiredAt,
        String statusReason
) {
}
