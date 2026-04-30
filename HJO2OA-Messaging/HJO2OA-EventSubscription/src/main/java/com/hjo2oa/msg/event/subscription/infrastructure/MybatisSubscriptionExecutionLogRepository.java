package com.hjo2oa.msg.event.subscription.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionExecutionLog;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionExecutionLogRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisSubscriptionExecutionLogRepository implements SubscriptionExecutionLogRepository {

    private final SubscriptionExecutionLogMapper mapper;

    public MybatisSubscriptionExecutionLogRepository(SubscriptionExecutionLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public SubscriptionExecutionLog saveIfAbsent(SubscriptionExecutionLog log) {
        return findByEventRuleRecipient(log.eventId(), log.ruleCode(), log.recipientId())
                .orElseGet(() -> {
                    mapper.insert(toEntity(log));
                    return log;
                });
    }

    @Override
    public Optional<SubscriptionExecutionLog> findByEventRuleRecipient(UUID eventId, String ruleCode, String recipientId) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<SubscriptionExecutionLogEntity>lambdaQuery()
                .eq(SubscriptionExecutionLogEntity::getEventId, eventId)
                .eq(SubscriptionExecutionLogEntity::getRuleCode, ruleCode)
                .eq(recipientId != null, SubscriptionExecutionLogEntity::getRecipientId, recipientId)
                .isNull(recipientId == null, SubscriptionExecutionLogEntity::getRecipientId)))
                .map(this::toDomain);
    }

    @Override
    public List<SubscriptionExecutionLog> findByEventId(UUID eventId) {
        return mapper.selectList(Wrappers.<SubscriptionExecutionLogEntity>lambdaQuery()
                        .eq(SubscriptionExecutionLogEntity::getEventId, eventId)
                        .orderByAsc(SubscriptionExecutionLogEntity::getOccurredAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private SubscriptionExecutionLogEntity toEntity(SubscriptionExecutionLog log) {
        return new SubscriptionExecutionLogEntity()
                .setId(log.id())
                .setEventId(log.eventId())
                .setEventType(log.eventType())
                .setRuleCode(log.ruleCode())
                .setRecipientId(log.recipientId())
                .setResult(log.result())
                .setMessage(log.message())
                .setTenantId(log.tenantId())
                .setOccurredAt(log.occurredAt());
    }

    private SubscriptionExecutionLog toDomain(SubscriptionExecutionLogEntity entity) {
        return new SubscriptionExecutionLog(
                entity.getId(),
                entity.getEventId(),
                entity.getEventType(),
                entity.getRuleCode(),
                entity.getRecipientId(),
                entity.getResult(),
                entity.getMessage(),
                entity.getTenantId(),
                entity.getOccurredAt()
        );
    }
}
