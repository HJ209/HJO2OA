package com.hjo2oa.todo.center.infrastructure.persistence;

import com.hjo2oa.todo.center.domain.TodoActionLogRepository;
import java.time.Instant;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisTodoActionLogRepository implements TodoActionLogRepository {

    private final TodoActionLogMapper mapper;

    public MybatisTodoActionLogRepository(TodoActionLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean registerIfAbsent(String idempotencyKey, String actionType, String targetId, Instant processedAt) {
        if (mapper.selectById(idempotencyKey) != null) {
            return false;
        }
        TodoActionLogEntity entity = new TodoActionLogEntity();
        entity.setIdempotencyKey(idempotencyKey);
        entity.setActionType(actionType);
        entity.setTargetId(targetId);
        entity.setProcessedAt(processedAt);
        mapper.insert(entity);
        return true;
    }
}
