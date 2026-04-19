package com.hjo2oa.portal.aggregation.api.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PortalCardSnapshotRepository {

    Optional<PortalCardSnapshot<?>> findByKey(PortalAggregationSnapshotKey snapshotKey);

    void save(PortalCardSnapshot<?> snapshot);

    int markStale(PortalSnapshotScope scope, Set<PortalCardType> cardTypes, String reason, Instant staleAt);

    List<PortalCardSnapshot<?>> findAll();
}
