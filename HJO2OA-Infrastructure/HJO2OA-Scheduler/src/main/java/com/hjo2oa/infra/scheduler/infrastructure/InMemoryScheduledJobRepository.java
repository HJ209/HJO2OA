package com.hjo2oa.infra.scheduler.infrastructure;

import com.hjo2oa.infra.scheduler.domain.ScheduledJob;
import com.hjo2oa.infra.scheduler.domain.ScheduledJobRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryScheduledJobRepository implements ScheduledJobRepository {

    private final Map<UUID, ScheduledJob> jobsById = new ConcurrentHashMap<>();

    @Override
    public Optional<ScheduledJob> findById(UUID id) {
        return Optional.ofNullable(jobsById.get(id));
    }

    @Override
    public Optional<ScheduledJob> findByJobCode(String jobCode) {
        return jobsById.values().stream()
                .filter(job -> job.jobCode().equals(jobCode))
                .findFirst();
    }

    @Override
    public List<ScheduledJob> findAll() {
        return jobsById.values().stream().toList();
    }

    @Override
    public List<ScheduledJob> findByTenantId(UUID tenantId) {
        return jobsById.values().stream()
                .filter(job -> tenantId.equals(job.tenantId()))
                .toList();
    }

    @Override
    public ScheduledJob save(ScheduledJob scheduledJob) {
        jobsById.put(scheduledJob.id(), scheduledJob);
        return scheduledJob;
    }
}
