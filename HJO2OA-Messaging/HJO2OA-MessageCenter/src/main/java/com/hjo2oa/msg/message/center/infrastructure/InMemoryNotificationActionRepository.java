package com.hjo2oa.msg.message.center.infrastructure;

import com.hjo2oa.msg.message.center.domain.NotificationAction;
import com.hjo2oa.msg.message.center.domain.NotificationActionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryNotificationActionRepository implements NotificationActionRepository {

    private final Map<String, List<NotificationAction>> actionsByNotificationId = new ConcurrentHashMap<>();

    @Override
    public NotificationAction save(NotificationAction action) {
        actionsByNotificationId
                .computeIfAbsent(action.notificationId(), ignored -> new ArrayList<>())
                .add(action);
        return action;
    }

    @Override
    public List<NotificationAction> findByNotificationId(String notificationId) {
        return List.copyOf(actionsByNotificationId.getOrDefault(notificationId, List.of()));
    }
}
