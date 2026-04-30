package com.hjo2oa.infra.scheduler.infrastructure;

import com.hjo2oa.infra.scheduler.application.SchedulerJobLock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemorySchedulerJobLock implements SchedulerJobLock {

    private final ConcurrentMap<UUID, UUID> lockedJobs = new ConcurrentHashMap<>();

    @Override
    public Optional<Lease> tryAcquire(UUID jobId, Duration ttl) {
        UUID leaseId = UUID.randomUUID();
        UUID previous = lockedJobs.putIfAbsent(jobId, leaseId);
        if (previous != null) {
            return Optional.empty();
        }
        return Optional.of(() -> lockedJobs.remove(jobId, leaseId));
    }
}
