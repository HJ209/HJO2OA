package com.hjo2oa.data.data.sync.infrastructure;

import com.hjo2oa.data.data.sync.domain.PagedResult;
import com.hjo2oa.data.data.sync.domain.SyncExchangeTask;
import com.hjo2oa.data.data.sync.domain.SyncExchangeTaskRepository;
import com.hjo2oa.data.data.sync.domain.SyncMode;
import com.hjo2oa.data.data.sync.domain.SyncTaskFilter;
import com.hjo2oa.data.data.sync.domain.SyncTaskStatus;
import javax.sql.DataSource;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemorySyncExchangeTaskRepository implements SyncExchangeTaskRepository {

    private final Map<UUID, SyncExchangeTask> store = new ConcurrentHashMap<>();

    @Override
    public Optional<SyncExchangeTask> findById(UUID taskId) {
        return Optional.ofNullable(store.get(taskId));
    }

    @Override
    public Optional<SyncExchangeTask> findByCode(UUID tenantId, String code) {
        return store.values().stream()
                .filter(task -> task.tenantId().equals(tenantId) && task.code().equals(code))
                .findFirst();
    }

    @Override
    public PagedResult<SyncExchangeTask> page(SyncTaskFilter filter) {
        List<SyncExchangeTask> filtered = store.values().stream()
                .filter(task -> filter.tenantId() == null || task.tenantId().equals(filter.tenantId()))
                .filter(task -> filter.code() == null || task.code().contains(filter.code()))
                .filter(task -> filter.syncMode() == null || task.syncMode() == filter.syncMode())
                .filter(task -> filter.status() == null || task.status() == filter.status())
                .filter(task -> filter.sourceConnectorId() == null || task.sourceConnectorId().equals(filter.sourceConnectorId()))
                .filter(task -> filter.targetConnectorId() == null || task.targetConnectorId().equals(filter.targetConnectorId()))
                .sorted(Comparator.comparing(SyncExchangeTask::updatedAt).reversed())
                .toList();
        return page(filtered, filter.page(), filter.size());
    }

    @Override
    public List<SyncExchangeTask> findByConnectorId(UUID connectorId) {
        return store.values().stream()
                .filter(task -> task.sourceConnectorId().equals(connectorId) || task.targetConnectorId().equals(connectorId))
                .sorted(Comparator.comparing(SyncExchangeTask::updatedAt).reversed())
                .toList();
    }

    @Override
    public List<SyncExchangeTask> findActiveEventDrivenTasks() {
        return store.values().stream()
                .filter(task -> task.status() == SyncTaskStatus.ACTIVE && task.syncMode() == SyncMode.EVENT_DRIVEN)
                .sorted(Comparator.comparing(SyncExchangeTask::updatedAt).reversed())
                .toList();
    }

    @Override
    public List<SyncExchangeTask> findActiveScheduledTasks() {
        return store.values().stream()
                .filter(task -> task.status() == SyncTaskStatus.ACTIVE)
                .filter(task -> task.scheduleConfig().enabled() || task.triggerConfig().schedulerJobCode() != null)
                .sorted(Comparator.comparing(SyncExchangeTask::updatedAt).reversed())
                .toList();
    }

    @Override
    public void save(SyncExchangeTask task) {
        store.put(task.taskId(), task);
    }

    @Override
    public void delete(UUID taskId) {
        store.remove(taskId);
    }

    private PagedResult<SyncExchangeTask> page(List<SyncExchangeTask> items, int page, int size) {
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(items.size(), from + size);
        List<SyncExchangeTask> pagedItems = from >= items.size() ? List.of() : items.subList(from, to);
        return new PagedResult<>(pagedItems, page, size, items.size());
    }
}
