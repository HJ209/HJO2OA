package com.hjo2oa.data.connector.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConnectorDefinitionRepository {

    Optional<ConnectorDefinition> findById(String connectorId);

    Optional<ConnectorDefinition> findByCode(String tenantId, String code);

    ConnectorPageResult findPage(
            String tenantId,
            ConnectorType connectorType,
            ConnectorStatus status,
            String code,
            String keyword,
            int page,
            int size
    );

    ConnectorDefinition save(ConnectorDefinition connectorDefinition);

    ConnectorHealthSnapshot saveHealthSnapshot(ConnectorHealthSnapshot healthSnapshot);

    Optional<ConnectorHealthSnapshot> findHealthSnapshotById(String snapshotId);

    Optional<ConnectorHealthSnapshot> findLatestSnapshot(String connectorId, ConnectorCheckType checkType);

    List<ConnectorHealthSnapshot> findSnapshots(
            String connectorId,
            ConnectorCheckType checkType,
            ConnectorHealthStatus healthStatus,
            Instant checkedFrom,
            Instant checkedTo,
            int limit
    );
}
