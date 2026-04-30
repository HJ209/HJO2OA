package com.hjo2oa.todo.center.infrastructure;

import com.hjo2oa.todo.center.domain.TodoActionLogRepository;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTodoActionLogRepository implements TodoActionLogRepository {

    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    @Override
    public boolean registerIfAbsent(String idempotencyKey, String actionType, String targetId, Instant processedAt) {
        return processedKeys.add(idempotencyKey);
    }
}
