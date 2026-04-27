package com.hjo2oa.wf.action.engine.infrastructure;

import com.hjo2oa.wf.action.engine.domain.TaskAction;
import com.hjo2oa.wf.action.engine.domain.TaskActionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTaskActionRepository implements TaskActionRepository {

    private final Map<UUID, TaskAction> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<TaskAction> findByIdempotency(UUID taskId, String actionCode, String idempotencyKey) {
        return storage.values().stream()
                .filter(action -> action.taskId().equals(taskId))
                .filter(action -> action.actionCode().equals(actionCode))
                .filter(action -> action.idempotencyKey().equals(idempotencyKey))
                .findFirst();
    }

    @Override
    public TaskAction save(TaskAction taskAction) {
        storage.put(taskAction.id(), taskAction);
        return taskAction;
    }

    @Override
    public List<TaskAction> findByTaskId(UUID taskId) {
        return storage.values().stream()
                .filter(action -> action.taskId().equals(taskId))
                .sorted(Comparator.comparing(TaskAction::createdAt).reversed())
                .toList();
    }
}
