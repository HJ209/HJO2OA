package com.hjo2oa.msg.message.center.domain;

import java.util.List;

public record NotificationBulkReadResult(
        int requestedCount,
        int processedCount,
        int readCount,
        int alreadyReadCount,
        int missingCount,
        List<NotificationSummary> notifications,
        List<String> missingNotificationIds
) {

    public NotificationBulkReadResult {
        notifications = List.copyOf(notifications);
        missingNotificationIds = List.copyOf(missingNotificationIds);
    }
}
