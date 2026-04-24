package com.hjo2oa.data.data.sync.infrastructure;

import com.hjo2oa.data.data.sync.domain.ExecutionStatus;
import com.hjo2oa.data.data.sync.domain.PagedResult;
import com.hjo2oa.data.data.sync.domain.SyncExecutionFilter;
import com.hjo2oa.data.data.sync.domain.SyncExecutionRecord;
import com.hjo2oa.data.data.sync.domain.SyncExecutionRecordRepository;
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
public class InMemorySyncExecutionRecordRepository implements SyncExecutionRecordRepository {

    private final Map<UUID, SyncExecutionRecord> store = new ConcurrentHashMap<>();

    @Override
    public Optional<SyncExecutionRecord> findById(UUID executionId) {
        return Optional.ofNullable(store.get(executionId));
    }

    @Override
    public Optional<SyncExecutionRecord> findByTaskIdAndIdempotencyKey(UUID taskId, String idempotencyKey) {
        return store.values().stream()
                .filter(record -> record.syncTaskId().equals(taskId))
                .filter(record -> idempotencyKey.equals(record.idempotencyKey()))
                .sorted(Comparator.comparing(SyncExecutionRecord::startedAt).reversed())
                .findFirst();
    }

    @Override
    public Optional<SyncExecutionRecord> findLatestByTaskId(UUID taskId) {
        return store.values().stream()
                .filter(record -> record.syncTaskId().equals(taskId))
                .max(Comparator.comparing(SyncExecutionRecord::startedAt));
    }

    @Override
    public Optional<SyncExecutionRecord> findLatestSuccessfulByTaskId(UUID taskId) {
        return store.values().stream()
                .filter(record -> record.syncTaskId().equals(taskId))
                .filter(record -> record.executionStatus() == ExecutionStatus.SUCCESS)
                .max(Comparator.comparing(SyncExecutionRecord::startedAt));
    }

    @Override
    public PagedResult<SyncExecutionRecord> page(SyncExecutionFilter filter) {
        List<SyncExecutionRecord> filtered = store.values().stream()
                .filter(record -> filter.taskId() == null || record.syncTaskId().equals(filter.taskId()))
                .filter(record -> filter.taskCode() == null || record.taskCode().contains(filter.taskCode()))
                .filter(record -> filter.executionStatus() == null || record.executionStatus() == filter.executionStatus())
                .filter(record -> filter.triggerType() == null || record.triggerType() == filter.triggerType())
                .filter(record -> filter.startedFrom() == null || !record.startedAt().isBefore(filter.startedFrom()))
                .filter(record -> filter.startedTo() == null || !record.startedAt().isAfter(filter.startedTo()))
                .sorted(Comparator.comparing(SyncExecutionRecord::startedAt).reversed())
                .toList();
        int from = Math.max(0, (filter.page() - 1) * filter.size());
        int to = Math.min(filtered.size(), from + filter.size());
        List<SyncExecutionRecord> paged = from >= filtered.size() ? List.of() : filtered.subList(from, to);
        return new PagedResult<>(paged, filter.page(), filter.size(), filtered.size());
    }

    @Override
    public void save(SyncExecutionRecord executionRecord) {
        store.put(executionRecord.executionId(), executionRecord);
    }
}
