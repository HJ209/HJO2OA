package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@ConditionalOnProfile
@ConditionalOnProperty(prefix = "hjo2oa.messaging.outbox.amqp", name = "enabled", havingValue = "true")
@Repository
public class MybatisEventOutboxRepository implements EventOutboxRepository {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_FAILED = "FAILED";

    private final EventOutboxMapper mapper;

    public MybatisEventOutboxRepository(EventOutboxMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public void save(EventOutboxEntity entity) {
        mapper.insert(entity);
    }

    @Override
    public List<EventOutboxEntity> findPending(int limit) {
        return mapper.selectList(Wrappers.<EventOutboxEntity>lambdaQuery()
                .in(EventOutboxEntity::getStatus, STATUS_PENDING, STATUS_FAILED)
                .orderByAsc(EventOutboxEntity::getCreatedAt)
                .last("OFFSET 0 ROWS FETCH NEXT " + limit + " ROWS ONLY"));
    }

    @Override
    public void markPublished(UUID id) {
        EventOutboxEntity entity = new EventOutboxEntity()
                .setId(id)
                .setStatus(STATUS_PUBLISHED)
                .setPublishedAt(Instant.now());
        mapper.updateById(entity);
    }

    @Override
    public void markFailed(UUID id) {
        EventOutboxEntity existing = mapper.selectById(id);
        if (existing == null) {
            return;
        }
        EventOutboxEntity entity = new EventOutboxEntity()
                .setId(id)
                .setStatus(STATUS_FAILED)
                .setRetryCount(existing.getRetryCount() == null ? 1 : existing.getRetryCount() + 1);
        mapper.updateById(entity);
    }
}
