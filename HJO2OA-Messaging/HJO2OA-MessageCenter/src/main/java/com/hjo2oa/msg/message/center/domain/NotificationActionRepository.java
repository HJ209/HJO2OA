package com.hjo2oa.msg.message.center.domain;

import java.util.List;

public interface NotificationActionRepository {

    NotificationAction save(NotificationAction action);

    List<NotificationAction> findByNotificationId(String notificationId);
}
