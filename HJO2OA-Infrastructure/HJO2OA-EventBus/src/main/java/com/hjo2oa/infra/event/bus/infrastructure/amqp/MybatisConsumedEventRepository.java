package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisConsumedEventRepository implements ConsumedEventRepository {

    private final ConsumedEventMapper mapper;

    public MybatisConsumedEventRepository(ConsumedEventMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public boolean tryStart(DomainEventEnvelope envelope, String consumerCode, Instant now) {
        Objects.requireNonNull(envelope, "envelope must not be null");
        Objects.requireNonNull(now, "now must not be null");
        ConsumedEventEntity existing = find(envelope.eventId(), consumerCode);
        if (existing != null && ConsumedEventStatus.FAILED.name().equals(existing.getStatus())) {
            mapper.updateById(existing
                    .setStatus(ConsumedEventStatus.PROCESSING.name())
                    .setLastError(null)
                    .setUpdatedAt(now));
            return true;
        }
        if (existing != null) {
            return false;
        }
        try {
            mapper.insert(new ConsumedEventEntity()
                    .setId(UUID.randomUUID())
                    .setEventId(envelope.eventId())
                    .setEventType(envelope.eventType())
                    .setConsumerCode(consumerCode)
                    .setTenantId(envelope.tenantId())
                    .setTraceId(envelope.traceId())
                    .setStatus(ConsumedEventStatus.PROCESSING.name())
                    .setCreatedAt(now)
                    .setUpdatedAt(now));
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    @Override
    public void markSuccess(UUID eventId, String consumerCode, Instant now) {
        ConsumedEventEntity existing = find(eventId, consumerCode);
        if (existing == null) {
            return;
        }
        mapper.updateById(existing
                .setStatus(ConsumedEventStatus.SUCCESS.name())
                .setConsumedAt(now)
                .setLastError(null)
                .setUpdatedAt(now));
    }

    @Override
    public void markFailed(UUID eventId, String consumerCode, String lastError, Instant now) {
        ConsumedEventEntity existing = find(eventId, consumerCode);
        if (existing == null) {
            return;
        }
        mapper.updateById(existing
                .setStatus(ConsumedEventStatus.FAILED.name())
                .setLastError(limitError(lastError))
                .setUpdatedAt(now));
    }

    private ConsumedEventEntity find(UUID eventId, String consumerCode) {
        return mapper.selectOne(Wrappers.<ConsumedEventEntity>lambdaQuery()
                .eq(ConsumedEventEntity::getEventId, eventId)
                .eq(ConsumedEventEntity::getConsumerCode, consumerCode));
    }

    private String limitError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 4000 ? error : error.substring(0, 4000);
    }
}
