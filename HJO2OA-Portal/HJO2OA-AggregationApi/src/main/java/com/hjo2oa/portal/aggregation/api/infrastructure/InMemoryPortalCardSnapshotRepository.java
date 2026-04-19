package com.hjo2oa.portal.aggregation.api.infrastructure;

import com.hjo2oa.portal.aggregation.api.domain.PortalAggregationSnapshotKey;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshot;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshotRepository;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotScope;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPortalCardSnapshotRepository implements PortalCardSnapshotRepository {

    private final Map<String, PortalCardSnapshot<?>> snapshots = new ConcurrentHashMap<>();

    @Override
    public Optional<PortalCardSnapshot<?>> findByKey(PortalAggregationSnapshotKey snapshotKey) {
        return Optional.ofNullable(snapshots.get(snapshotKey.asCacheKey()));
    }

    @Override
    public void save(PortalCardSnapshot<?> snapshot) {
        snapshots.put(snapshot.snapshotKey().asCacheKey(), snapshot);
    }

    @Override
    public int markStale(PortalSnapshotScope scope, Set<PortalCardType> cardTypes, String reason, Instant staleAt) {
        int updatedCount = 0;
        for (Map.Entry<String, PortalCardSnapshot<?>> entry : snapshots.entrySet()) {
            PortalCardSnapshot<?> snapshot = entry.getValue();
            boolean cardMatches = cardTypes == null || cardTypes.isEmpty() || cardTypes.contains(snapshot.cardType());
            if (!cardMatches || !scope.matches(snapshot.snapshotKey())) {
                continue;
            }
            entry.setValue(snapshot.markStale(reason, staleAt));
            updatedCount++;
        }
        return updatedCount;
    }

    @Override
    public List<PortalCardSnapshot<?>> findAll() {
        return List.copyOf(snapshots.values());
    }
}
