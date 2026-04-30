package com.hjo2oa.infra.scheduler.application;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface SchedulerJobLock {

    Optional<Lease> tryAcquire(UUID jobId, Duration ttl);

    interface Lease extends AutoCloseable {

        @Override
        void close();
    }
}
