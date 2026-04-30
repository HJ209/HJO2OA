package com.hjo2oa.content.statistics.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentStatisticsRepository {

    boolean hasAction(UUID tenantId, String idempotencyKey);

    void recordAction(ContentStatisticsApplicationService.ContentActionRecord record);

    long countActions(UUID tenantId, UUID articleId, ContentStatisticsApplicationService.ContentActionType actionType);

    long countUniqueReaders(UUID tenantId, UUID articleId);

    Optional<ContentStatisticsApplicationService.ContentEngagementSnapshot> findSnapshot(
            UUID tenantId,
            UUID articleId,
            String bucket
    );

    void saveSnapshot(ContentStatisticsApplicationService.ContentEngagementSnapshot snapshot);

    List<ContentStatisticsApplicationService.ContentEngagementSnapshot> ranking(
            UUID tenantId,
            String bucket,
            int limit,
            Instant now
    );
}
