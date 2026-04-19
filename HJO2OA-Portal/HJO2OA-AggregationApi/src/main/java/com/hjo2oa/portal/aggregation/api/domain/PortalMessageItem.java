package com.hjo2oa.portal.aggregation.api.domain;

import java.time.Instant;

public record PortalMessageItem(
        String notificationId,
        String title,
        String category,
        String priority,
        String deepLink,
        Instant createdAt
) {
}
