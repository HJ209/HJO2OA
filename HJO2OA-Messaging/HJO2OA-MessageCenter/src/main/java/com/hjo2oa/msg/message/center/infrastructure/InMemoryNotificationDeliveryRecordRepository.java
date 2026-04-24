package com.hjo2oa.msg.message.center.infrastructure;

import com.hjo2oa.msg.message.center.domain.NotificationDeliveryChannel;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecord;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecordRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryNotificationDeliveryRecordRepository implements NotificationDeliveryRecordRepository {

    private final Map<String, NotificationDeliveryRecord> deliveryByKey = new ConcurrentHashMap<>();

    @Override
    public Optional<NotificationDeliveryRecord> findByNotificationIdAndChannel(
            String notificationId,
            NotificationDeliveryChannel channel
    ) {
        return Optional.ofNullable(deliveryByKey.get(buildKey(notificationId, channel)));
    }

    @Override
    public NotificationDeliveryRecord save(NotificationDeliveryRecord deliveryRecord) {
        deliveryByKey.put(buildKey(deliveryRecord.notificationId(), deliveryRecord.channel()), deliveryRecord);
        return deliveryRecord;
    }

    @Override
    public List<NotificationDeliveryRecord> findByNotificationId(String notificationId) {
        return deliveryByKey.values().stream()
                .filter(record -> record.notificationId().equals(notificationId))
                .toList();
    }

    private static String buildKey(String notificationId, NotificationDeliveryChannel channel) {
        return notificationId + "::" + channel.name();
    }
}
