package com.hjo2oa.wf.process.instance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskInstanceRepository {

    Optional<TaskInstance> findById(UUID taskId);

    List<TaskInstance> findByInstanceId(UUID instanceId);

    List<TaskInstance> findOpenByInstanceId(UUID instanceId);

    List<TaskInstance> findOpenByNode(UUID instanceId, String nodeId);

    TaskInstance save(TaskInstance task);

    List<TaskInstance> saveAll(List<TaskInstance> tasks);
}
