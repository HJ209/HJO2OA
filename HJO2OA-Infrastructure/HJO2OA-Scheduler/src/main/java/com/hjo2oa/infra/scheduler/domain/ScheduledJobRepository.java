package com.hjo2oa.infra.scheduler.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduledJobRepository {

    Optional<ScheduledJob> findById(UUID id);

    Optional<ScheduledJob> findByJobCode(String jobCode);

    List<ScheduledJob> findAll();

    List<ScheduledJob> findByTenantId(UUID tenantId);

    ScheduledJob save(ScheduledJob scheduledJob);
}
