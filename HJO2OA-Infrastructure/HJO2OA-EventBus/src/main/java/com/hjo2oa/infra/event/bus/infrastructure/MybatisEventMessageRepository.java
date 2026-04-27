package com.hjo2oa.infra.event.bus.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.infra.event.bus.domain.DeliveryAttempt;
import com.hjo2oa.infra.event.bus.domain.DeliveryStatus;
import com.hjo2oa.infra.event.bus.domain.EventMessage;
import com.hjo2oa.infra.event.bus.domain.EventMessageRepository;
import com.hjo2oa.infra.event.bus.domain.PublishStatus;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisEventMessageRepository implements EventMessageRepository {

    private final EventMessageMapper eventMessageMapper;
    private final DeliveryAttemptMapper deliveryAttemptMapper;

    public MybatisEventMessageRepository(
            EventMessageMapper eventMessageMapper,
            DeliveryAttemptMapper deliveryAttemptMapper
    ) {
        this.eventMessageMapper = Objects.requireNonNull(eventMessageMapper, "eventMessageMapper must not be null");
        this.deliveryAttemptMapper = Objects.requireNonNull(deliveryAttemptMapper, "deliveryAttemptMapper must not be null");
    }

    @Override
    public EventMessage save(EventMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        EventMessageEntity entity = toEntity(message);
        EventMessageEntity existing = eventMessageMapper.selectById(message.id());
        if (existing == null) {
            eventMessageMapper.insert(entity);
        } else {
            eventMessageMapper.updateById(entity);
        }
        saveDeliveryAttempts(message);
        return message;
    }

    @Override
    public Optional<EventMessage> findById(UUID id) {
        EventMessageEntity entity = eventMessageMapper.selectById(id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(loadAggregate(entity));
    }

    @Override
    public List<EventMessage> findByPublishStatus(PublishStatus status) {
        LambdaQueryWrapper<EventMessageEntity> wrapper = Wrappers.<EventMessageEntity>lambdaQuery()
                .eq(EventMessageEntity::getPublishStatus, status.name());
        return eventMessageMapper.selectList(wrapper).stream()
                .map(this::loadAggregate)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventMessage> findByTenantId(UUID tenantId) {
        LambdaQueryWrapper<EventMessageEntity> wrapper = Wrappers.<EventMessageEntity>lambdaQuery()
                .eq(EventMessageEntity::getTenantId, tenantId);
        return eventMessageMapper.selectList(wrapper).stream()
                .map(this::loadAggregate)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventMessage> findByTraceId(String traceId) {
        LambdaQueryWrapper<EventMessageEntity> wrapper = Wrappers.<EventMessageEntity>lambdaQuery()
                .eq(EventMessageEntity::getTraceId, traceId);
        return eventMessageMapper.selectList(wrapper).stream()
                .map(this::loadAggregate)
                .collect(Collectors.toList());
    }

    private EventMessage loadAggregate(EventMessageEntity entity) {
        List<DeliveryAttempt> attempts = findDeliveryAttempts(entity.getId());
        return new EventMessage(
                entity.getId(),
                entity.getEventDefinitionId(),
                entity.getEventType(),
                entity.getSource(),
                entity.getTenantId(),
                entity.getCorrelationId(),
                entity.getTraceId(),
                entity.getOperatorAccountId(),
                entity.getOperatorPersonId(),
                entity.getPayload(),
                PublishStatus.valueOf(entity.getPublishStatus()),
                entity.getPublishedAt(),
                entity.getRetainedUntil(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                attempts
        );
    }

    private List<DeliveryAttempt> findDeliveryAttempts(UUID eventMessageId) {
        LambdaQueryWrapper<DeliveryAttemptEntity> wrapper = Wrappers.<DeliveryAttemptEntity>lambdaQuery()
                .eq(DeliveryAttemptEntity::getEventMessageId, eventMessageId)
                .orderByAsc(DeliveryAttemptEntity::getAttemptNo);
        return deliveryAttemptMapper.selectList(wrapper).stream()
                .map(this::toDeliveryAttempt)
                .collect(Collectors.toList());
    }

    private void saveDeliveryAttempts(EventMessage message) {
        for (DeliveryAttempt attempt : message.deliveryAttempts()) {
            DeliveryAttemptEntity entity = toDeliveryAttemptEntity(attempt);
            DeliveryAttemptEntity existing = deliveryAttemptMapper.selectById(attempt.id());
            if (existing == null) {
                deliveryAttemptMapper.insert(entity);
            } else {
                deliveryAttemptMapper.updateById(entity);
            }
        }
    }

    private EventMessageEntity toEntity(EventMessage message) {
        EventMessageEntity entity = new EventMessageEntity();
        entity.setId(message.id());
        entity.setEventDefinitionId(message.eventDefinitionId());
        entity.setEventType(message.eventType());
        entity.setSource(message.source());
        entity.setTenantId(message.tenantId());
        entity.setCorrelationId(message.correlationId());
        entity.setTraceId(message.traceId());
        entity.setOperatorAccountId(message.operatorAccountId());
        entity.setOperatorPersonId(message.operatorPersonId());
        entity.setPayload(message.payload());
        entity.setPublishStatus(message.publishStatus().name());
        entity.setPublishedAt(message.publishedAt());
        entity.setRetainedUntil(message.retainedUntil());
        entity.setCreatedAt(message.createdAt());
        entity.setUpdatedAt(message.updatedAt());
        return entity;
    }

    private DeliveryAttempt toDeliveryAttempt(DeliveryAttemptEntity entity) {
        return new DeliveryAttempt(
                entity.getId(),
                entity.getEventMessageId(),
                entity.getSubscriberCode(),
                entity.getAttemptNo(),
                DeliveryStatus.valueOf(entity.getDeliveryStatus()),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getDeliveredAt(),
                entity.getNextRetryAt(),
                entity.getRequestSnapshot(),
                entity.getResponseSnapshot()
        );
    }

    private DeliveryAttemptEntity toDeliveryAttemptEntity(DeliveryAttempt attempt) {
        DeliveryAttemptEntity entity = new DeliveryAttemptEntity();
        entity.setId(attempt.id());
        entity.setEventMessageId(attempt.eventMessageId());
        entity.setSubscriberCode(attempt.subscriberCode());
        entity.setAttemptNo(attempt.attemptNo());
        entity.setDeliveryStatus(attempt.deliveryStatus().name());
        entity.setErrorCode(attempt.errorCode());
        entity.setErrorMessage(attempt.errorMessage());
        entity.setDeliveredAt(attempt.deliveredAt());
        entity.setNextRetryAt(attempt.nextRetryAt());
        entity.setRequestSnapshot(attempt.requestSnapshot());
        entity.setResponseSnapshot(attempt.responseSnapshot());
        return entity;
    }
}
