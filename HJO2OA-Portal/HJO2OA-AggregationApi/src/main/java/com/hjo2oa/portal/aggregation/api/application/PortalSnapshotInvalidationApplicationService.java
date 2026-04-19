package com.hjo2oa.portal.aggregation.api.application;

import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshotRepository;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotScope;
import java.time.Clock;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PortalSnapshotInvalidationApplicationService {

    private final PortalCardSnapshotRepository snapshotRepository;
    private final Clock clock;

    public PortalSnapshotInvalidationApplicationService(PortalCardSnapshotRepository snapshotRepository) {
        this(snapshotRepository, Clock.systemUTC());
    }

    public PortalSnapshotInvalidationApplicationService(
            PortalCardSnapshotRepository snapshotRepository,
            Clock clock
    ) {
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository, "snapshotRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public int markStale(PortalSnapshotScope scope, Set<PortalCardType> cardTypes, String reason) {
        return snapshotRepository.markStale(scope, cardTypes, reason, clock.instant());
    }
}
