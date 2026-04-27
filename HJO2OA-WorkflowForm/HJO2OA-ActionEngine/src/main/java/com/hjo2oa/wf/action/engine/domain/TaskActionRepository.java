package com.hjo2oa.wf.action.engine.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskActionRepository {

    Optional<TaskAction> findByIdempotency(UUID taskId, String actionCode, String idempotencyKey);

    TaskAction save(TaskAction taskAction);

    List<TaskAction> findByTaskId(UUID taskId);
}
