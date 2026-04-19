package com.hjo2oa.todo.center.infrastructure;

import com.hjo2oa.todo.center.domain.TodoProjectionEventLog;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTodoProjectionEventLog implements TodoProjectionEventLog {

    private final Set<UUID> processedEvents = ConcurrentHashMap.newKeySet();

    @Override
    public boolean registerIfAbsent(UUID eventId) {
        return processedEvents.add(eventId);
    }
}
