package com.hjo2oa.todo.center.infrastructure.persistence;

import com.hjo2oa.todo.center.domain.TodoProjectionEventLog;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisTodoProjectionEventLog implements TodoProjectionEventLog {

    private final TodoProjectionEventMapper mapper;

    public MybatisTodoProjectionEventLog(TodoProjectionEventMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean registerIfAbsent(UUID eventId) {
        TodoProjectionEventEntity entity = new TodoProjectionEventEntity();
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
