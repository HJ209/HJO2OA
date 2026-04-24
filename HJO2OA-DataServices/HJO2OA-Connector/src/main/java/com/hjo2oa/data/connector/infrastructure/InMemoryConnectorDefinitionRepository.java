package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.ConnectorDefinition;
import com.hjo2oa.data.connector.domain.ConnectorDefinitionRepository;
import com.hjo2oa.data.connector.domain.ConnectorCheckType;
import com.hjo2oa.data.connector.domain.ConnectorHealthSnapshot;
import com.hjo2oa.data.connector.domain.ConnectorHealthStatus;
import com.hjo2oa.data.connector.domain.ConnectorPageResult;
import com.hjo2oa.data.connector.domain.ConnectorStatus;
import com.hjo2oa.data.connector.domain.ConnectorType;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryConnectorDefinitionRepository implements ConnectorDefinitionRepository {

    private final Map<String, ConnectorDefinition> definitionsById = new ConcurrentHashMap<>();
    private final Map<String, ConnectorHealthSnapshot> latestHealthSnapshots = new ConcurrentHashMap<>();
    private final Map<String, List<ConnectorHealthSnapshot>> healthHistory = new ConcurrentHashMap<>();

    @Override
    public Optional<ConnectorDefinition> findById(String connectorId) {
        return Optional.ofNullable(definitionsById.get(connectorId));
    }

    @Override
    public Optional<ConnectorDefinition> findByCode(String tenantId, String code) {
        return definitionsById.values().stream()
                .filter(connector -> connector.tenantId().equals(tenantId))
                .filter(connector -> connector.code().equals(code))
                .findFirst();
    }

    @Override
    public ConnectorPageResult findPage(
            String tenantId,
            ConnectorType connectorType,
            ConnectorStatus status,
            String code,
            String keyword,
            int page,
            int size
    ) {
        List<ConnectorDefinition> filtered = definitionsById.values().stream()
                .filter(connector -> connector.tenantId().equals(tenantId))
                .filter(connector -> connectorType == null || connector.connectorType() == connectorType)
                .filter(connector -> status == null || connector.status() == status)
                .filter(connector -> code == null || code.isBlank() || connector.code().equals(code))
                .filter(connector -> keyword == null
                        || keyword.isBlank()
                        || connector.code().contains(keyword)
                        || connector.name().contains(keyword))
                .sorted(Comparator.comparing(ConnectorDefinition::code))
                .toList();
        int fromIndex = Math.min(filtered.size(), (page - 1) * size);
        int toIndex = Math.min(filtered.size(), fromIndex + size);
        return new ConnectorPageResult(filtered.subList(fromIndex, toIndex), filtered.size());
    }

    @Override
    public ConnectorDefinition save(ConnectorDefinition connectorDefinition) {
        definitionsById.put(connectorDefinition.connectorId(), connectorDefinition);
        return connectorDefinition;
    }

    @Override
    public ConnectorHealthSnapshot saveHealthSnapshot(ConnectorHealthSnapshot healthSnapshot) {
        List<ConnectorHealthSnapshot> snapshots = new ArrayList<>(
                healthHistory.getOrDefault(healthSnapshot.connectorId(), List.of())
        );
        snapshots.removeIf(existing -> existing.snapshotId().equals(healthSnapshot.snapshotId()));
        snapshots.add(healthSnapshot);
        snapshots.sort(Comparator.comparing(ConnectorHealthSnapshot::checkedAt).reversed());
        healthHistory.put(healthSnapshot.connectorId(), List.copyOf(snapshots));
        latestHealthSnapshots.put(healthSnapshot.connectorId(), snapshots.get(0));
        return healthSnapshot;
    }

    @Override
    public Optional<ConnectorHealthSnapshot> findHealthSnapshotById(String snapshotId) {
        return healthHistory.values().stream()
                .flatMap(List::stream)
                .filter(snapshot -> snapshot.snapshotId().equals(snapshotId))
                .findFirst();
    }

    @Override
    public Optional<ConnectorHealthSnapshot> findLatestSnapshot(String connectorId, ConnectorCheckType checkType) {
        return healthHistory.getOrDefault(connectorId, List.of()).stream()
                .filter(snapshot -> snapshot.checkType() == checkType)
                .findFirst();
    }

    @Override
    public List<ConnectorHealthSnapshot> findSnapshots(
            String connectorId,
            ConnectorCheckType checkType,
            ConnectorHealthStatus healthStatus,
            Instant checkedFrom,
            Instant checkedTo,
            int limit
    ) {
        return healthHistory.getOrDefault(connectorId, List.of()).stream()
                .filter(snapshot -> checkType == null || snapshot.checkType() == checkType)
                .filter(snapshot -> healthStatus == null || snapshot.healthStatus() == healthStatus)
                .filter(snapshot -> checkedFrom == null || !snapshot.checkedAt().isBefore(checkedFrom))
                .filter(snapshot -> checkedTo == null || !snapshot.checkedAt().isAfter(checkedTo))
                .limit(limit)
                .toList();
    }
}
