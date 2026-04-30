package com.hjo2oa.msg.message.center.application;

import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationInboxStatus;
import java.util.List;

public record MessageNotificationQuery(
        NotificationInboxStatus inboxStatus,
        String readStatus,
        String messageType,
        String sourceModule
) {

    public List<NotificationCategory> requestedCategories() {
        if (messageType == null || messageType.isBlank() || "ALL".equalsIgnoreCase(messageType)) {
            return List.of();
        }
        return switch (messageType.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "TASK" -> List.of(
                    NotificationCategory.TODO_CREATED,
                    NotificationCategory.TODO_REMINDER,
                    NotificationCategory.TODO_COMPLETED
            );
            case "ALERT" -> List.of(
                    NotificationCategory.TODO_OVERDUE,
                    NotificationCategory.TODO_REMINDER,
                    NotificationCategory.PROCESS_TASK_OVERDUE
            );
            case "SYSTEM" -> List.of(NotificationCategory.ORG_ACCOUNT_LOCKED, NotificationCategory.SYSTEM_SECURITY);
            case "APPROVAL" -> List.of(
                    NotificationCategory.APPROVAL,
                    NotificationCategory.TODO_CREATED,
                    NotificationCategory.TODO_COMPLETED
            );
            case "NOTICE" -> List.of(NotificationCategory.BUSINESS_NOTICE);
            default -> List.of();
        };
    }
}
