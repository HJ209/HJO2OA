package com.hjo2oa.org.org.sync.audit.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyncSourceConfigRepository {

    SyncSourceConfig save(SyncSourceConfig config);

    Optional<SyncSourceConfig> findById(UUID sourceId);

    Optional<SyncSourceConfig> findByTenantIdAndSourceCode(UUID tenantId, String sourceCode);

    List<SyncSourceConfig> findByTenantId(UUID tenantId);
}
