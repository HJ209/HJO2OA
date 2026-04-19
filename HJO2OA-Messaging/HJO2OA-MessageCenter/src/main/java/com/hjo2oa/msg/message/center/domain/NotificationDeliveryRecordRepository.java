package com.hjo2oa.msg.message.center.domain;

import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryRecordRepository {

    Optional<NotificationDeliveryRecord> findByNotificationIdAndChannel(
            String notificationId,
            NotificationDeliveryChannel channel
    );

    NotificationDeliveryRecord save(NotificationDeliveryRecord deliveryRecord);

    List<NotificationDeliveryRecord> findByNotificationId(String notificationId);
}
