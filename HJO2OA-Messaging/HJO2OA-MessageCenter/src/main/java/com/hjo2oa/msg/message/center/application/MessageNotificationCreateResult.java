package com.hjo2oa.msg.message.center.application;

import com.hjo2oa.msg.message.center.domain.NotificationSummary;

public record MessageNotificationCreateResult(
        NotificationSummary notification,
        boolean created
) {
}
