package com.hjo2oa.msg.message.center.infrastructure;

import com.hjo2oa.msg.message.center.domain.NotificationProjectionEventLog;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryNotificationProjectionEventLog implements NotificationProjectionEventLog {

    private final Set<UUID> processedEventIds = ConcurrentHashMap.newKeySet();

    @Override
    public boolean registerIfAbsent(UUID eventId) {
        return processedEventIds.add(eventId);
    }
}
