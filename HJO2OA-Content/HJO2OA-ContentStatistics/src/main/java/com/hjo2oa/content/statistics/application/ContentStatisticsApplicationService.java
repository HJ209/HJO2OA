package com.hjo2oa.content.statistics.application;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentStatisticsApplicationService {

    public static final String DEFAULT_BUCKET = "ALL_TIME";

    private final ContentStatisticsRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public ContentStatisticsApplicationService(ContentStatisticsRepository repository) {
        this(repository, event -> {
        }, Clock.systemUTC());
    }

    public ContentStatisticsApplicationService(
            ContentStatisticsRepository repository,
            DomainEventPublisher eventPublisher,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public ContentEngagementSnapshotView recordAction(RecordContentActionCommand command) {
        validate(command.tenantId(), command.articleId());
        if (command.idempotencyKey() != null
                && !command.idempotencyKey().isBlank()
                && repository.hasAction(command.tenantId(), command.idempotencyKey())) {
            return snapshot(command.tenantId(), command.articleId(), DEFAULT_BUCKET);
        }
        Instant now = clock.instant();
        repository.recordAction(new ContentActionRecord(
                UUID.randomUUID(),
                command.articleId(),
                command.tenantId(),
                command.personId(),
                command.assignmentId(),
                command.actionType(),
                command.idempotencyKey(),
                now
        ));
        ContentEngagementSnapshot snapshot = aggregate(command.tenantId(), command.articleId(), DEFAULT_BUCKET, now);
        eventPublisher.publish(new ContentStatisticsChangedEvent(
                UUID.randomUUID(),
                "content.statistics.changed",
                now,
                command.tenantId().toString(),
                command.articleId(),
                snapshot.hotScore()
        ));
        return toView(snapshot);
    }

    @Transactional
    public ContentEngagementSnapshot aggregate(UUID tenantId, UUID articleId, String bucket, Instant now) {
        long readCount = repository.countActions(tenantId, articleId, ContentActionType.READ);
        long uniqueReaders = repository.countUniqueReaders(tenantId, articleId);
        long downloadCount = repository.countActions(tenantId, articleId, ContentActionType.DOWNLOAD);
        long favoriteCount = repository.countActions(tenantId, articleId, ContentActionType.FAVORITE);
        BigDecimal hotScore = BigDecimal.valueOf(readCount)
                .add(BigDecimal.valueOf(uniqueReaders).multiply(BigDecimal.valueOf(2)))
                .add(BigDecimal.valueOf(downloadCount).multiply(BigDecimal.valueOf(3)))
                .add(BigDecimal.valueOf(favoriteCount).multiply(BigDecimal.valueOf(4)))
                .setScale(4, RoundingMode.HALF_UP);
        ContentEngagementSnapshot snapshot = repository.findSnapshot(tenantId, articleId, bucket)
                .map(existing -> existing.withCounts(readCount, uniqueReaders, downloadCount, favoriteCount, hotScore, now))
                .orElseGet(() -> new ContentEngagementSnapshot(
                        UUID.randomUUID(),
                        articleId,
                        tenantId,
                        bucket,
                        readCount,
                        uniqueReaders,
                        downloadCount,
                        favoriteCount,
                        hotScore,
                        now,
                        now,
                        now
                ));
        repository.saveSnapshot(snapshot);
        return snapshot;
    }

    @Transactional(readOnly = true)
    public ContentEngagementSnapshotView snapshot(UUID tenantId, UUID articleId, String bucket) {
        validate(tenantId, articleId);
        return repository.findSnapshot(tenantId, articleId, defaultBucket(bucket))
                .map(this::toView)
                .orElseGet(() -> toView(new ContentEngagementSnapshot(
                        UUID.randomUUID(),
                        articleId,
                        tenantId,
                        defaultBucket(bucket),
                        0,
                        0,
                        0,
                        0,
                        BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                        null,
                        clock.instant(),
                        clock.instant()
                )));
    }

    @Transactional(readOnly = true)
    public List<ContentEngagementSnapshotView> ranking(UUID tenantId, String bucket, int limit) {
        if (tenantId == null) {
            throw new BizException(SharedErrorDescriptors.TENANT_REQUIRED, "Tenant id is required");
        }
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return repository.ranking(tenantId, defaultBucket(bucket), safeLimit, clock.instant()).stream()
                .map(this::toView)
                .toList();
    }

    private ContentEngagementSnapshotView toView(ContentEngagementSnapshot snapshot) {
        return new ContentEngagementSnapshotView(
                snapshot.articleId(),
                snapshot.tenantId(),
                snapshot.bucket(),
                snapshot.readCount(),
                snapshot.uniqueReaderCount(),
                snapshot.downloadCount(),
                snapshot.favoriteCount(),
                snapshot.hotScore(),
                snapshot.lastAggregatedAt()
        );
    }

    private static String defaultBucket(String bucket) {
        return bucket == null || bucket.isBlank() ? DEFAULT_BUCKET : bucket.trim().toUpperCase();
    }

    private static void validate(UUID tenantId, UUID articleId) {
        if (tenantId == null) {
            throw new BizException(SharedErrorDescriptors.TENANT_REQUIRED, "Tenant id is required");
        }
        if (articleId == null) {
            throw new BizException(SharedErrorDescriptors.VALIDATION_ERROR, "Article id is required");
        }
    }

    public enum ContentActionType {
        READ,
        DOWNLOAD,
        FAVORITE
    }

    public record ContentActionRecord(
            UUID id,
            UUID articleId,
            UUID tenantId,
            UUID personId,
            UUID assignmentId,
            ContentActionType actionType,
            String idempotencyKey,
            Instant occurredAt
    ) {
    }

    public record ContentEngagementSnapshot(
            UUID id,
            UUID articleId,
            UUID tenantId,
            String bucket,
            long readCount,
            long uniqueReaderCount,
            long downloadCount,
            long favoriteCount,
            BigDecimal hotScore,
            Instant lastAggregatedAt,
            Instant createdAt,
            Instant updatedAt
    ) {

        ContentEngagementSnapshot withCounts(
                long newReadCount,
                long newUniqueReaderCount,
                long newDownloadCount,
                long newFavoriteCount,
                BigDecimal newHotScore,
                Instant now
        ) {
            return new ContentEngagementSnapshot(
                    id,
                    articleId,
                    tenantId,
                    bucket,
                    newReadCount,
                    newUniqueReaderCount,
                    newDownloadCount,
                    newFavoriteCount,
                    newHotScore,
                    now,
                    createdAt,
                    now
            );
        }
    }

    public record RecordContentActionCommand(
            UUID tenantId,
            UUID articleId,
            UUID personId,
            UUID assignmentId,
            ContentActionType actionType,
            String idempotencyKey
    ) {

        public RecordContentActionCommand {
            actionType = actionType == null ? ContentActionType.READ : actionType;
        }
    }

    public record ContentEngagementSnapshotView(
            UUID articleId,
            UUID tenantId,
            String bucket,
            long readCount,
            long uniqueReaderCount,
            long downloadCount,
            long favoriteCount,
            BigDecimal hotScore,
            Instant lastAggregatedAt
    ) {
    }

    public record ContentStatisticsChangedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID articleId,
            BigDecimal hotScore
    ) implements DomainEvent {
    }
}
