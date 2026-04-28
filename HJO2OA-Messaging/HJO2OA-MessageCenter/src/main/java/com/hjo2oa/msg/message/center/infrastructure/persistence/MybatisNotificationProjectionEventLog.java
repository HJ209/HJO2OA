package com.hjo2oa.msg.message.center.infrastructure.persistence;

import com.hjo2oa.msg.message.center.domain.NotificationProjectionEventLog;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisNotificationProjectionEventLog implements NotificationProjectionEventLog {

    private final NotificationProjectionEventMapper mapper;

    public MybatisNotificationProjectionEventLog(NotificationProjectionEventMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean registerIfAbsent(UUID eventId) {
        NotificationProjectionEventEntity entity = new NotificationProjectionEventEntity();
        entity.setEventId(eventId);
        entity.setProcessedAt(Instant.now());
        try {
            mapper.insert(entity);
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }
}
