package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisEventOutboxRepository implements EventOutboxRepository {

    private final EventOutboxMapper mapper;

    public MybatisEventOutboxRepository(EventOutboxMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public void save(EventOutboxEntity entity) {
        mapper.insert(entity);
    }

    @Override
    public List<EventOutboxEntity> findDueForPublish(Instant now, int limit) {
        Objects.requireNonNull(now, "now must not be null");
        return mapper.selectList(Wrappers.<EventOutboxEntity>lambdaQuery()
                .and(criteria -> criteria
                        .eq(EventOutboxEntity::getStatus, EventOutboxStatus.PENDING.name())
                        .or(statusCriteria -> statusCriteria
                                .eq(EventOutboxEntity::getStatus, EventOutboxStatus.FAILED.name())
                                .and(retryCriteria -> retryCriteria
                                        .isNull(EventOutboxEntity::getNextRetryAt)
                                        .or()
                                        .le(EventOutboxEntity::getNextRetryAt, now))))
                .orderByAsc(EventOutboxEntity::getCreatedAt)
                .last(fetchFirst(limit)));
    }

    @Override
    public Optional<EventOutboxEntity> findByEventId(UUID eventId) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        EventOutboxEntity entity = mapper.selectOne(Wrappers.<EventOutboxEntity>lambdaQuery()
                .eq(EventOutboxEntity::getEventId, eventId));
        return Optional.ofNullable(entity);
    }

    @Override
    public EventOutboxPage query(EventOutboxQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        LambdaQueryWrapper<EventOutboxEntity> countWrapper = buildWrapper(query);
        long total = mapper.selectCount(countWrapper);
        LambdaQueryWrapper<EventOutboxEntity> pageWrapper = buildWrapper(query)
                .orderByDesc(EventOutboxEntity::getCreatedAt)
                .last(pageClause(query.page(), query.size()));
        return new EventOutboxPage(mapper.selectList(pageWrapper), total);
    }

    @Override
    public EventOutboxStatistics statistics() {
        long pending = countByStatus(EventOutboxStatus.PENDING);
        long published = countByStatus(EventOutboxStatus.PUBLISHED);
        long failed = countByStatus(EventOutboxStatus.FAILED);
        long dead = countByStatus(EventOutboxStatus.DEAD);
        return new EventOutboxStatistics(pending, published, failed, dead, pending + published + failed + dead);
    }

    @Override
    public void markPublished(UUID id, Instant publishedAt) {
        Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        EventOutboxEntity entity = new EventOutboxEntity()
                .setId(id)
                .setStatus(EventOutboxStatus.PUBLISHED.name())
                .setPublishedAt(publishedAt)
                .setNextRetryAt(null)
                .setLastError(null)
                .setDeadAt(null);
        mapper.updateById(entity);
    }

    @Override
    public void markFailed(UUID id, int retryCount, Instant nextRetryAt, String lastError) {
        EventOutboxEntity entity = new EventOutboxEntity()
                .setId(id)
                .setStatus(EventOutboxStatus.FAILED.name())
                .setRetryCount(retryCount)
                .setNextRetryAt(nextRetryAt)
                .setLastError(limitError(lastError))
                .setDeadAt(null);
        mapper.updateById(entity);
    }

    @Override
    public void markDead(UUID id, int retryCount, String lastError, Instant deadAt) {
        EventOutboxEntity entity = new EventOutboxEntity()
                .setId(id)
                .setStatus(EventOutboxStatus.DEAD.name())
                .setRetryCount(retryCount)
                .setNextRetryAt(null)
                .setLastError(limitError(lastError))
                .setDeadAt(deadAt);
        mapper.updateById(entity);
    }

    @Override
    public void resetForReplay(UUID id, Instant now) {
        EventOutboxEntity entity = new EventOutboxEntity()
                .setId(id)
                .setStatus(EventOutboxStatus.PENDING.name())
                .setRetryCount(0)
                .setNextRetryAt(now)
                .setPublishedAt(null)
                .setLastError(null)
                .setDeadAt(null);
        mapper.updateById(entity);
    }

    private LambdaQueryWrapper<EventOutboxEntity> buildWrapper(EventOutboxQuery query) {
        LambdaQueryWrapper<EventOutboxEntity> wrapper = Wrappers.lambdaQuery();
        if (query.eventId() != null) {
            wrapper.eq(EventOutboxEntity::getEventId, query.eventId());
        }
        if (hasText(query.eventType())) {
            wrapper.eq(EventOutboxEntity::getEventType, query.eventType());
        }
        if (hasText(query.aggregateType())) {
            wrapper.eq(EventOutboxEntity::getAggregateType, query.aggregateType());
        }
        if (hasText(query.aggregateId())) {
            wrapper.eq(EventOutboxEntity::getAggregateId, query.aggregateId());
        }
        if (hasText(query.tenantId())) {
            wrapper.eq(EventOutboxEntity::getTenantId, query.tenantId());
        }
        if (hasText(query.traceId())) {
            wrapper.eq(EventOutboxEntity::getTraceId, query.traceId());
        }
        if (query.status() != null) {
            wrapper.eq(EventOutboxEntity::getStatus, query.status().name());
        }
        if (query.occurredFrom() != null) {
            wrapper.ge(EventOutboxEntity::getOccurredAt, query.occurredFrom());
        }
        if (query.occurredTo() != null) {
            wrapper.le(EventOutboxEntity::getOccurredAt, query.occurredTo());
        }
        return wrapper;
    }

    private long countByStatus(EventOutboxStatus status) {
        return mapper.selectCount(Wrappers.<EventOutboxEntity>lambdaQuery()
                .eq(EventOutboxEntity::getStatus, status.name()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String fetchFirst(int limit) {
        int normalizedLimit = Math.min(Math.max(limit, 1), 500);
        return "OFFSET 0 ROWS FETCH NEXT " + normalizedLimit + " ROWS ONLY";
    }

    private String pageClause(int page, int size) {
        int offset = (page - 1) * size;
        return "OFFSET " + offset + " ROWS FETCH NEXT " + size + " ROWS ONLY";
    }

    private String limitError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 4000 ? error : error.substring(0, 4000);
    }
}
