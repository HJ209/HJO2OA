package com.hjo2oa.wf.action.engine.infrastructure.process;

import com.hjo2oa.wf.action.engine.domain.TaskInstanceGateway;
import com.hjo2oa.wf.action.engine.domain.TaskInstanceSnapshot;
import com.hjo2oa.wf.action.engine.domain.TaskStatus;
import com.hjo2oa.wf.process.instance.domain.TaskInstance;
import com.hjo2oa.wf.process.instance.domain.TaskInstanceRepository;
import com.hjo2oa.wf.process.instance.domain.TaskInstanceStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class ProcessInstanceTaskInstanceGateway implements TaskInstanceGateway {

    private final TaskInstanceRepository taskRepository;
    private final Clock clock;

    @Autowired
    public ProcessInstanceTaskInstanceGateway(TaskInstanceRepository taskRepository) {
        this(taskRepository, Clock.systemUTC());
    }

    ProcessInstanceTaskInstanceGateway(TaskInstanceRepository taskRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    @Override
    public Optional<TaskInstanceSnapshot> findById(UUID taskId) {
        return taskRepository.findById(taskId).map(this::toSnapshot);
    }

    @Override
    public TaskInstanceSnapshot updateStatus(UUID taskId, TaskStatus status) {
        TaskInstance task = taskRepository.findById(taskId).orElseThrow();
        Instant now = clock.instant();
        TaskInstance updated = switch (status) {
            case COMPLETED -> task.complete(now);
            case REJECTED, TERMINATED -> task.terminate(now);
            default -> task;
        };
        return toSnapshot(taskRepository.save(updated));
    }

    @Override
    public TaskInstanceSnapshot transfer(UUID taskId, String assigneeId) {
        TaskInstance task = taskRepository.findById(taskId).orElseThrow();
        TaskInstance updated = task.transfer(
                parseUuid(assigneeId),
                task.assigneeOrgId(),
                task.assigneeDeptId(),
                task.assigneePositionId(),
                clock.instant()
        );
        return toSnapshot(taskRepository.save(updated));
    }

    @Override
    public TaskInstanceSnapshot addSign(UUID taskId, String assigneeId) {
        return updateStatus(taskId, TaskStatus.ADD_SIGNED);
    }

    @Override
    public TaskInstanceSnapshot reduceSign(UUID taskId, String assigneeId) {
        return updateStatus(taskId, TaskStatus.REDUCE_SIGNED);
    }

    private TaskInstanceSnapshot toSnapshot(TaskInstance task) {
        return new TaskInstanceSnapshot(
                task.id(),
                task.instanceId(),
                task.assigneeId() == null ? null : task.assigneeId().toString(),
                toActionStatus(task.status()),
                task.tenantId().toString()
        );
    }

    private TaskStatus toActionStatus(TaskInstanceStatus status) {
        return switch (status) {
            case CREATED, CLAIMED -> TaskStatus.PENDING;
            case COMPLETED -> TaskStatus.COMPLETED;
            case TERMINATED -> TaskStatus.TERMINATED;
            case TRANSFERRED -> TaskStatus.TRANSFERRED;
        };
    }

    private UUID parseUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }
}
