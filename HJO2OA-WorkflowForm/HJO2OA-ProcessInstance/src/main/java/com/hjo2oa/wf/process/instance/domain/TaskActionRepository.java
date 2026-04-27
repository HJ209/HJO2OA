package com.hjo2oa.wf.process.instance.domain;

import java.util.List;
import java.util.UUID;

public interface TaskActionRepository {

    List<TaskAction> findByInstanceId(UUID instanceId);

    List<TaskAction> findByTaskId(UUID taskId);

    TaskAction save(TaskAction action);
}
