package com.hjo2oa.org.org.sync.audit.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyncTaskRepository {

    SyncTask save(SyncTask task);

    Optional<SyncTask> findById(UUID taskId);

    boolean existsActiveTask(UUID tenantId, UUID sourceId);

    List<SyncTask> findByTenantId(UUID tenantId);

    List<SyncTask> findByTenantIdAndSourceId(UUID tenantId, UUID sourceId);
}
