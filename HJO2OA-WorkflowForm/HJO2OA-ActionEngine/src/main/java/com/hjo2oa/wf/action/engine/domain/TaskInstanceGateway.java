package com.hjo2oa.wf.action.engine.domain;

import java.util.Optional;
import java.util.UUID;

public interface TaskInstanceGateway {

    Optional<TaskInstanceSnapshot> findById(UUID taskId);

    TaskInstanceSnapshot updateStatus(UUID taskId, TaskStatus status);

    TaskInstanceSnapshot transfer(UUID taskId, String assigneeId);

    TaskInstanceSnapshot addSign(UUID taskId, String assigneeId);

    TaskInstanceSnapshot reduceSign(UUID taskId, String assigneeId);
}
