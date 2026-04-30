package com.hjo2oa.content.statistics.infrastructure;

import com.hjo2oa.content.statistics.application.ContentStatisticsApplicationService.ContentActionRecord;
import com.hjo2oa.content.statistics.application.ContentStatisticsApplicationService.ContentActionType;
import com.hjo2oa.content.statistics.application.ContentStatisticsApplicationService.ContentEngagementSnapshot;
import com.hjo2oa.content.statistics.application.ContentStatisticsRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryContentStatisticsRepository implements ContentStatisticsRepository {

    private final Map<UUID, ContentActionRecord> records = new ConcurrentHashMap<>();
    private final Map<String, ContentEngagementSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public boolean hasAction(UUID tenantId, String idempotencyKey) {
        return idempotencyKey != null
                && records.values().stream()
                .anyMatch(record -> record.tenantId().equals(tenantId) && idempotencyKey.equals(record.idempotencyKey()));
    }

    @Override
    public void recordAction(ContentActionRecord record) {
        records.put(record.id(), record);
    }

    @Override
    public long countActions(UUID tenantId, UUID articleId, ContentActionType actionType) {
        return records.values().stream()
                .filter(record -> record.tenantId().equals(tenantId))
                .filter(record -> record.articleId().equals(articleId))
                .filter(record -> record.actionType() == actionType)
                .count();
    }

    @Override
    public long countUniqueReaders(UUID tenantId, UUID articleId) {
        return records.values().stream()
                .filter(record -> record.tenantId().equals(tenantId))
                .filter(record -> record.articleId().equals(articleId))
                .filter(record -> record.actionType() == ContentActionType.READ)
                .map(ContentActionRecord::personId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
    }

    @Override
    public Optional<ContentEngagementSnapshot> findSnapshot(UUID tenantId, UUID articleId, String bucket) {
        return Optional.ofNullable(snapshots.get(key(tenantId, articleId, bucket)));
    }

    @Override
    public void saveSnapshot(ContentEngagementSnapshot snapshot) {
        snapshots.put(key(snapshot.tenantId(), snapshot.articleId(), snapshot.bucket()), snapshot);
    }

    @Override
    public List<ContentEngagementSnapshot> ranking(UUID tenantId, String bucket, int limit, Instant now) {
        return snapshots.values().stream()
                .filter(snapshot -> snapshot.tenantId().equals(tenantId))
                .filter(snapshot -> snapshot.bucket().equals(bucket))
                .sorted(Comparator.comparing(ContentEngagementSnapshot::hotScore).reversed())
                .limit(limit)
                .toList();
    }

    private static String key(UUID tenantId, UUID articleId, String bucket) {
        return tenantId + ":" + articleId + ":" + bucket;
    }
}
