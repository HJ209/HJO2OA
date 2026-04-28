package com.hjo2oa.infra.event.bus.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.infra.event.bus.domain.EventDefinition;
import com.hjo2oa.infra.event.bus.domain.EventDefinitionRepository;
import com.hjo2oa.infra.event.bus.domain.EventDefinitionStatus;
import com.hjo2oa.infra.event.bus.domain.MatchMode;
import com.hjo2oa.infra.event.bus.domain.PublishMode;
import com.hjo2oa.infra.event.bus.domain.SubscriptionBinding;
import com.hjo2oa.infra.event.bus.domain.TenantScope;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisEventDefinitionRepository implements EventDefinitionRepository {

    private final EventDefinitionMapper eventDefinitionMapper;
    private final SubscriptionBindingMapper subscriptionBindingMapper;

    public MybatisEventDefinitionRepository(
            EventDefinitionMapper eventDefinitionMapper,
            SubscriptionBindingMapper subscriptionBindingMapper
    ) {
        this.eventDefinitionMapper = Objects.requireNonNull(eventDefinitionMapper, "eventDefinitionMapper must not be null");
        this.subscriptionBindingMapper = Objects.requireNonNull(subscriptionBindingMapper, "subscriptionBindingMapper must not be null");
    }

    @Override
    public EventDefinition save(EventDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        EventDefinitionEntity entity = toEntity(definition);
        EventDefinitionEntity existing = eventDefinitionMapper.selectById(definition.id());
        if (existing == null) {
            eventDefinitionMapper.insert(entity);
        } else {
            eventDefinitionMapper.updateById(entity);
        }
        saveSubscriptions(definition);
        return definition;
    }

    @Override
    public Optional<EventDefinition> findById(UUID id) {
        EventDefinitionEntity entity = eventDefinitionMapper.selectById(id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(loadAggregate(entity));
    }

    @Override
    public Optional<EventDefinition> findByEventTypeAndVersion(String eventType, String version) {
        LambdaQueryWrapper<EventDefinitionEntity> wrapper = Wrappers.<EventDefinitionEntity>lambdaQuery()
                .eq(EventDefinitionEntity::getEventType, eventType)
                .eq(EventDefinitionEntity::getVersion, version);
        EventDefinitionEntity entity = eventDefinitionMapper.selectOne(wrapper);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(loadAggregate(entity));
    }

    @Override
    public List<EventDefinition> findByModulePrefix(String modulePrefix) {
        LambdaQueryWrapper<EventDefinitionEntity> wrapper = Wrappers.<EventDefinitionEntity>lambdaQuery()
                .eq(EventDefinitionEntity::getModulePrefix, modulePrefix);
        return eventDefinitionMapper.selectList(wrapper).stream()
                .map(this::loadAggregate)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventDefinition> findAll() {
        return eventDefinitionMapper.selectList(null).stream()
                .map(this::loadAggregate)
                .collect(Collectors.toList());
    }

    private EventDefinition loadAggregate(EventDefinitionEntity entity) {
        List<SubscriptionBinding> subscriptions = findSubscriptions(entity.getId());
        return new EventDefinition(
                entity.getId(),
                entity.getEventType(),
                entity.getModulePrefix(),
                entity.getVersion(),
                entity.getPayloadSchema(),
                entity.getDescription(),
                PublishMode.valueOf(entity.getPublishMode()),
                EventDefinitionStatus.valueOf(entity.getStatus()),
                entity.getOwnerModule(),
                TenantScope.valueOf(entity.getTenantScope()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                subscriptions
        );
    }

    private List<SubscriptionBinding> findSubscriptions(UUID eventDefinitionId) {
        LambdaQueryWrapper<SubscriptionBindingEntity> wrapper = Wrappers.<SubscriptionBindingEntity>lambdaQuery()
                .eq(SubscriptionBindingEntity::getEventDefinitionId, eventDefinitionId);
        return subscriptionBindingMapper.selectList(wrapper).stream()
                .map(this::toSubscriptionBinding)
                .collect(Collectors.toList());
    }

    private void saveSubscriptions(EventDefinition definition) {
        for (SubscriptionBinding subscription : definition.subscriptions()) {
            SubscriptionBindingEntity entity = toSubscriptionEntity(subscription);
            SubscriptionBindingEntity existing = subscriptionBindingMapper.selectById(subscription.id());
            if (existing == null) {
                subscriptionBindingMapper.insert(entity);
            } else {
                subscriptionBindingMapper.updateById(entity);
            }
        }
    }

    private EventDefinitionEntity toEntity(EventDefinition definition) {
        EventDefinitionEntity entity = new EventDefinitionEntity();
        entity.setId(definition.id());
        entity.setEventType(definition.eventType());
        entity.setModulePrefix(definition.modulePrefix());
        entity.setVersion(definition.version());
        entity.setPayloadSchema(definition.payloadSchema());
        entity.setDescription(definition.description());
        entity.setPublishMode(definition.publishMode().name());
        entity.setStatus(definition.status().name());
        entity.setOwnerModule(definition.ownerModule());
        entity.setTenantScope(definition.tenantScope().name());
        entity.setCreatedAt(definition.createdAt());
        entity.setUpdatedAt(definition.updatedAt());
        return entity;
    }

    private SubscriptionBinding toSubscriptionBinding(SubscriptionBindingEntity entity) {
        return new SubscriptionBinding(
                entity.getId(),
                entity.getEventDefinitionId(),
                entity.getSubscriberCode(),
                MatchMode.valueOf(entity.getMatchMode()),
                entity.getRetryPolicy(),
                Boolean.TRUE.equals(entity.getDeadLetterEnabled()),
                Boolean.TRUE.equals(entity.getActive())
        );
    }

    private SubscriptionBindingEntity toSubscriptionEntity(SubscriptionBinding binding) {
        SubscriptionBindingEntity entity = new SubscriptionBindingEntity();
        entity.setId(binding.id());
        entity.setEventDefinitionId(binding.eventDefinitionId());
        entity.setSubscriberCode(binding.subscriberCode());
        entity.setMatchMode(binding.matchMode().name());
        entity.setRetryPolicy(binding.retryPolicy());
        entity.setDeadLetterEnabled(binding.deadLetterEnabled());
        entity.setActive(binding.active());
        return entity;
    }
}
