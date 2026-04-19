package com.hjo2oa.msg.message.center.infrastructure;

import com.hjo2oa.msg.message.center.domain.Notification;
import com.hjo2oa.msg.message.center.domain.NotificationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryNotificationRepository implements NotificationRepository {

    private final Map<String, Notification> notificationsById = new ConcurrentHashMap<>();
    private final Map<String, String> notificationIdByDedupKey = new ConcurrentHashMap<>();

    @Override
    public Optional<Notification> findByNotificationId(String notificationId) {
        return Optional.ofNullable(notificationsById.get(notificationId));
    }

    @Override
    public Optional<Notification> findByDedupKey(String dedupKey) {
        String notificationId = notificationIdByDedupKey.get(dedupKey);
        if (notificationId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(notificationsById.get(notificationId));
    }

    @Override
    public Notification save(Notification notification) {
        notificationsById.put(notification.notificationId(), notification);
        notificationIdByDedupKey.put(notification.dedupKey(), notification.notificationId());
        return notification;
    }

    @Override
    public List<Notification> findAll() {
        return new ArrayList<>(notificationsById.values());
    }
}
