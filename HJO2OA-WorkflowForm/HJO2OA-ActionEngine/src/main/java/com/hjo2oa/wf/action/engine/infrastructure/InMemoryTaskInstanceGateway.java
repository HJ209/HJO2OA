package com.hjo2oa.wf.action.engine.infrastructure;

import com.hjo2oa.wf.action.engine.domain.TaskInstanceGateway;
import com.hjo2oa.wf.action.engine.domain.TaskInstanceSnapshot;
import com.hjo2oa.wf.action.engine.domain.TaskStatus;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTaskInstanceGateway implements TaskInstanceGateway {

    private final Map<UUID, TaskInstanceSnapshot> storage = new ConcurrentHashMap<>();

    public void save(TaskInstanceSnapshot task) {
        storage.put(task.taskId(), task);
    }

    @Override
    public Optional<TaskInstanceSnapshot> findById(UUID taskId) {
        return Optional.ofNullable(storage.get(taskId));
    }

    @Override
    public TaskInstanceSnapshot updateStatus(UUID taskId, TaskStatus status) {
        return storage.computeIfPresent(taskId, (id, task) -> new TaskInstanceSnapshot(
                task.taskId(),
                task.instanceId(),
                task.assigneeId(),
                status,
                task.tenantId()
        ));
    }

    @Override
    public TaskInstanceSnapshot transfer(UUID taskId, String assigneeId) {
        return storage.computeIfPresent(taskId, (id, task) -> new TaskInstanceSnapshot(
                task.taskId(),
                task.instanceId(),
                assigneeId,
                TaskStatus.TRANSFERRED,
                task.tenantId()
        ));
    }

    @Override
    public TaskInstanceSnapshot addSign(UUID taskId, String assigneeId) {
        return updateStatus(taskId, TaskStatus.ADD_SIGNED);
    }

    @Override
    public TaskInstanceSnapshot reduceSign(UUID taskId, String assigneeId) {
        return updateStatus(taskId, TaskStatus.REDUCE_SIGNED);
    }
}
