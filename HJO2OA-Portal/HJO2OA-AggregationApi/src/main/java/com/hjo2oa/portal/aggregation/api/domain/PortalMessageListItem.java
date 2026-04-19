package com.hjo2oa.portal.aggregation.api.domain;

import java.time.Instant;

public record PortalMessageListItem(
        String notificationId,
        String title,
        String bodySummary,
        String category,
        String priority,
        String inboxStatus,
        String deliveryStatus,
        String sourceModule,
        String deepLink,
        String targetAssignmentId,
        String targetPositionId,
        Instant createdAt
) {
}
