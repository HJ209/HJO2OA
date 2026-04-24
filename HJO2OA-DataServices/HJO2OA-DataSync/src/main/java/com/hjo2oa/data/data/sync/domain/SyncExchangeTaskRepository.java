package com.hjo2oa.data.data.sync.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyncExchangeTaskRepository {

    Optional<SyncExchangeTask> findById(UUID taskId);

    Optional<SyncExchangeTask> findByCode(UUID tenantId, String code);

    PagedResult<SyncExchangeTask> page(SyncTaskFilter filter);

    List<SyncExchangeTask> findByConnectorId(UUID connectorId);

    List<SyncExchangeTask> findActiveEventDrivenTasks();

    List<SyncExchangeTask> findActiveScheduledTasks();

    void save(SyncExchangeTask task);

    void delete(UUID taskId);
}
