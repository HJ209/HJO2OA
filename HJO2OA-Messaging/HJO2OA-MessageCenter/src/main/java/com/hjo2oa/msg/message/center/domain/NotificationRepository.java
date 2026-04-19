package com.hjo2oa.msg.message.center.domain;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    Optional<Notification> findByNotificationId(String notificationId);

    Optional<Notification> findByDedupKey(String dedupKey);

    Notification save(Notification notification);

    List<Notification> findAll();
}
