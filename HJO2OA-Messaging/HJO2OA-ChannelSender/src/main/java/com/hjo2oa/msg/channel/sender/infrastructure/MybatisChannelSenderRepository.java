package com.hjo2oa.msg.channel.sender.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.msg.channel.sender.domain.ChannelEndpoint;
import com.hjo2oa.msg.channel.sender.domain.ChannelEndpointStatus;
import com.hjo2oa.msg.channel.sender.domain.ChannelSenderRepository;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import com.hjo2oa.msg.channel.sender.domain.DeliveryAttempt;
import com.hjo2oa.msg.channel.sender.domain.DeliveryAttemptResultStatus;
import com.hjo2oa.msg.channel.sender.domain.DeliveryTask;
import com.hjo2oa.msg.channel.sender.domain.DeliveryTaskStatus;
import com.hjo2oa.msg.channel.sender.domain.MessageCategory;
import com.hjo2oa.msg.channel.sender.domain.MessagePriority;
import com.hjo2oa.msg.channel.sender.domain.MessageTemplate;
import com.hjo2oa.msg.channel.sender.domain.MessageTemplateStatus;
import com.hjo2oa.msg.channel.sender.domain.ProviderType;
import com.hjo2oa.msg.channel.sender.domain.QuietWindowBehavior;
import com.hjo2oa.msg.channel.sender.domain.RoutingPolicy;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisChannelSenderRepository implements ChannelSenderRepository {

    private final MessageTemplateMapper messageTemplateMapper;
    private final ChannelEndpointMapper channelEndpointMapper;
    private final RoutingPolicyMapper routingPolicyMapper;
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final DeliveryAttemptMapper deliveryAttemptMapper;

    public MybatisChannelSenderRepository(
            MessageTemplateMapper messageTemplateMapper,
            ChannelEndpointMapper channelEndpointMapper,
            RoutingPolicyMapper routingPolicyMapper,
            DeliveryTaskMapper deliveryTaskMapper,
            DeliveryAttemptMapper deliveryAttemptMapper
    ) {
        this.messageTemplateMapper = Objects.requireNonNull(messageTemplateMapper);
        this.channelEndpointMapper = Objects.requireNonNull(channelEndpointMapper);
        this.routingPolicyMapper = Objects.requireNonNull(routingPolicyMapper);
        this.deliveryTaskMapper = Objects.requireNonNull(deliveryTaskMapper);
        this.deliveryAttemptMapper = Objects.requireNonNull(deliveryAttemptMapper);
    }

    @Override
    public MessageTemplate saveTemplate(MessageTemplate template) {
        MessageTemplateEntity entity = toTemplateEntity(template);
        if (messageTemplateMapper.selectById(template.id()) == null) {
            messageTemplateMapper.insert(entity);
        } else {
            messageTemplateMapper.updateById(entity);
        }
        return toTemplate(messageTemplateMapper.selectById(template.id()));
    }

    @Override
    public Optional<MessageTemplate> findTemplateById(UUID templateId) {
        return Optional.ofNullable(messageTemplateMapper.selectById(templateId)).map(this::toTemplate);
    }

    @Override
    public Optional<MessageTemplate> findTemplate(
            UUID tenantId,
            String code,
            ChannelType channelType,
            String locale,
            int version
    ) {
        LambdaQueryWrapper<MessageTemplateEntity> wrapper = tenantCriteria(
                Wrappers.<MessageTemplateEntity>lambdaQuery(),
                MessageTemplateEntity::getTenantId,
                tenantId
        ).eq(MessageTemplateEntity::getCode, code)
                .eq(MessageTemplateEntity::getChannelType, channelType.name())
                .eq(MessageTemplateEntity::getLocale, normalizeLocale(locale))
                .eq(MessageTemplateEntity::getVersion, version);
        return Optional.ofNullable(messageTemplateMapper.selectOne(wrapper)).map(this::toTemplate);
    }

    @Override
    public List<MessageTemplate> findTemplates(UUID tenantId, MessageCategory category) {
        LambdaQueryWrapper<MessageTemplateEntity> wrapper = tenantCriteria(
                Wrappers.<MessageTemplateEntity>lambdaQuery(),
                MessageTemplateEntity::getTenantId,
                tenantId
        ).orderByAsc(MessageTemplateEntity::getCode)
                .orderByAsc(MessageTemplateEntity::getVersion);
        if (category != null) {
            wrapper.eq(MessageTemplateEntity::getCategory, category.name());
        }
        return messageTemplateMapper.selectList(wrapper).stream().map(this::toTemplate).toList();
    }

    @Override
    public ChannelEndpoint saveEndpoint(ChannelEndpoint endpoint) {
        ChannelEndpointEntity entity = toEndpointEntity(endpoint);
        if (channelEndpointMapper.selectById(endpoint.id()) == null) {
            channelEndpointMapper.insert(entity);
        } else {
            channelEndpointMapper.updateById(entity);
        }
        return toEndpoint(channelEndpointMapper.selectById(endpoint.id()));
    }

    @Override
    public Optional<ChannelEndpoint> findEndpointById(UUID endpointId) {
        return Optional.ofNullable(channelEndpointMapper.selectById(endpointId)).map(this::toEndpoint);
    }

    @Override
    public Optional<ChannelEndpoint> findEndpoint(UUID tenantId, String endpointCode) {
        LambdaQueryWrapper<ChannelEndpointEntity> wrapper = tenantCriteria(
                Wrappers.<ChannelEndpointEntity>lambdaQuery(),
                ChannelEndpointEntity::getTenantId,
                tenantId
        ).eq(ChannelEndpointEntity::getEndpointCode, endpointCode);
        return Optional.ofNullable(channelEndpointMapper.selectOne(wrapper)).map(this::toEndpoint);
    }

    @Override
    public List<ChannelEndpoint> findEndpoints(UUID tenantId, ChannelType channelType) {
        LambdaQueryWrapper<ChannelEndpointEntity> wrapper = tenantCriteria(
                Wrappers.<ChannelEndpointEntity>lambdaQuery(),
                ChannelEndpointEntity::getTenantId,
                tenantId
        ).orderByAsc(ChannelEndpointEntity::getEndpointCode);
        if (channelType != null) {
            wrapper.eq(ChannelEndpointEntity::getChannelType, channelType.name());
        }
        return channelEndpointMapper.selectList(wrapper).stream().map(this::toEndpoint).toList();
    }

    @Override
    public RoutingPolicy saveRoutingPolicy(RoutingPolicy policy) {
        RoutingPolicyEntity entity = toRoutingPolicyEntity(policy);
        if (routingPolicyMapper.selectById(policy.id()) == null) {
            routingPolicyMapper.insert(entity);
        } else {
            routingPolicyMapper.updateById(entity);
        }
        return toRoutingPolicy(routingPolicyMapper.selectById(policy.id()));
    }

    @Override
    public Optional<RoutingPolicy> findRoutingPolicyById(UUID policyId) {
        return Optional.ofNullable(routingPolicyMapper.selectById(policyId)).map(this::toRoutingPolicy);
    }

    @Override
    public Optional<RoutingPolicy> findRoutingPolicy(UUID tenantId, String policyCode) {
        LambdaQueryWrapper<RoutingPolicyEntity> wrapper = tenantCriteria(
                Wrappers.<RoutingPolicyEntity>lambdaQuery(),
                RoutingPolicyEntity::getTenantId,
                tenantId
        ).eq(RoutingPolicyEntity::getPolicyCode, policyCode);
        return Optional.ofNullable(routingPolicyMapper.selectOne(wrapper)).map(this::toRoutingPolicy);
    }

    @Override
    public List<RoutingPolicy> findEnabledRoutingPolicies(
            UUID tenantId,
            MessageCategory category,
            MessagePriority priority
    ) {
        LambdaQueryWrapper<RoutingPolicyEntity> wrapper = tenantCriteria(
                Wrappers.<RoutingPolicyEntity>lambdaQuery(),
                RoutingPolicyEntity::getTenantId,
                tenantId
        ).eq(RoutingPolicyEntity::getEnabled, true)
                .orderByAsc(RoutingPolicyEntity::getPolicyCode);
        if (category != null) {
            wrapper.eq(RoutingPolicyEntity::getCategory, category.name());
        }
        return routingPolicyMapper.selectList(wrapper).stream()
                .map(this::toRoutingPolicy)
                .filter(policy -> priority == null || priority.ordinal() >= policy.priorityThreshold().ordinal())
                .toList();
    }

    @Override
    public DeliveryTask saveDeliveryTask(DeliveryTask task) {
        DeliveryTaskEntity entity = toDeliveryTaskEntity(task);
        if (deliveryTaskMapper.selectById(task.id()) == null) {
            deliveryTaskMapper.insert(entity);
        } else {
            deliveryTaskMapper.updateById(entity);
        }
        syncAttempts(task);
        return loadTask(task.id());
    }

    @Override
    public Optional<DeliveryTask> findDeliveryTaskById(UUID taskId) {
        DeliveryTaskEntity entity = deliveryTaskMapper.selectById(taskId);
        return entity == null ? Optional.empty() : Optional.of(loadTask(taskId));
    }

    @Override
    public List<DeliveryTask> findRetryableTasks(UUID tenantId) {
        LambdaQueryWrapper<DeliveryTaskEntity> wrapper = tenantCriteria(
                Wrappers.<DeliveryTaskEntity>lambdaQuery(),
                DeliveryTaskEntity::getTenantId,
                tenantId
        ).eq(DeliveryTaskEntity::getStatus, DeliveryTaskStatus.FAILED.name())
                .le(DeliveryTaskEntity::getNextRetryAt, Instant.now())
                .orderByAsc(DeliveryTaskEntity::getNextRetryAt);
        return deliveryTaskMapper.selectList(wrapper).stream().map(entity -> loadTask(entity.getId())).toList();
    }

    private DeliveryTask loadTask(UUID taskId) {
        DeliveryTaskEntity entity = deliveryTaskMapper.selectById(taskId);
        List<DeliveryAttempt> attempts = deliveryAttemptMapper.selectList(
                Wrappers.<DeliveryAttemptEntity>lambdaQuery()
                        .eq(DeliveryAttemptEntity::getDeliveryTaskId, taskId)
                        .orderByAsc(DeliveryAttemptEntity::getAttemptNo)
        ).stream().map(this::toAttempt).toList();
        return toDeliveryTask(entity, attempts);
    }

    private void syncAttempts(DeliveryTask task) {
        for (DeliveryAttempt attempt : task.attempts()) {
            DeliveryAttemptEntity entity = toAttemptEntity(attempt);
            if (deliveryAttemptMapper.selectById(attempt.id()) == null) {
                deliveryAttemptMapper.insert(entity);
            } else {
                deliveryAttemptMapper.updateById(entity);
            }
        }
    }

    private MessageTemplateEntity toTemplateEntity(MessageTemplate template) {
        MessageTemplateEntity entity = new MessageTemplateEntity();
        entity.setId(template.id());
        entity.setCode(template.code());
        entity.setChannelType(template.channelType().name());
        entity.setLocale(template.locale());
        entity.setVersion(template.version());
        entity.setCategory(template.category().name());
        entity.setTitleTemplate(template.titleTemplate());
        entity.setBodyTemplate(template.bodyTemplate());
        entity.setVariableSchema(template.variableSchema());
        entity.setStatus(template.status().name());
        entity.setSystemLocked(template.systemLocked());
        entity.setTenantId(template.tenantId());
        entity.setCreatedAt(template.createdAt());
        entity.setUpdatedAt(template.updatedAt());
        return entity;
    }

    private MessageTemplate toTemplate(MessageTemplateEntity entity) {
        return new MessageTemplate(
                entity.getId(),
                entity.getCode(),
                ChannelType.valueOf(entity.getChannelType()),
                entity.getLocale(),
                entity.getVersion(),
                MessageCategory.valueOf(entity.getCategory()),
                entity.getTitleTemplate(),
                entity.getBodyTemplate(),
                entity.getVariableSchema(),
                MessageTemplateStatus.valueOf(entity.getStatus()),
                Boolean.TRUE.equals(entity.getSystemLocked()),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ChannelEndpointEntity toEndpointEntity(ChannelEndpoint endpoint) {
        ChannelEndpointEntity entity = new ChannelEndpointEntity();
        entity.setId(endpoint.id());
        entity.setEndpointCode(endpoint.endpointCode());
        entity.setChannelType(endpoint.channelType().name());
        entity.setProviderType(endpoint.providerType().name());
        entity.setDisplayName(endpoint.displayName());
        entity.setStatus(endpoint.status().name());
        entity.setConfigRef(endpoint.configRef());
        entity.setRateLimitPerMinute(endpoint.rateLimitPerMinute());
        entity.setDailyQuota(endpoint.dailyQuota());
        entity.setTenantId(endpoint.tenantId());
        entity.setCreatedAt(endpoint.createdAt());
        entity.setUpdatedAt(endpoint.updatedAt());
        return entity;
    }

    private ChannelEndpoint toEndpoint(ChannelEndpointEntity entity) {
        return new ChannelEndpoint(
                entity.getId(),
                entity.getEndpointCode(),
                ChannelType.valueOf(entity.getChannelType()),
                ProviderType.valueOf(entity.getProviderType()),
                entity.getDisplayName(),
                ChannelEndpointStatus.valueOf(entity.getStatus()),
                entity.getConfigRef(),
                entity.getRateLimitPerMinute(),
                entity.getDailyQuota(),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private RoutingPolicyEntity toRoutingPolicyEntity(RoutingPolicy policy) {
        RoutingPolicyEntity entity = new RoutingPolicyEntity();
        entity.setId(policy.id());
        entity.setPolicyCode(policy.policyCode());
        entity.setCategory(policy.category().name());
        entity.setPriorityThreshold(policy.priorityThreshold().name());
        entity.setTargetChannelOrder(toChannelJson(policy.targetChannelOrder()));
        entity.setFallbackChannelOrder(toChannelJson(policy.fallbackChannelOrder()));
        entity.setQuietWindowBehavior(policy.quietWindowBehavior().name());
        entity.setDedupWindowSeconds(policy.dedupWindowSeconds());
        entity.setEscalationPolicy(policy.escalationPolicy());
        entity.setEnabled(policy.enabled());
        entity.setTenantId(policy.tenantId());
        entity.setCreatedAt(policy.createdAt());
        entity.setUpdatedAt(policy.updatedAt());
        return entity;
    }

    private RoutingPolicy toRoutingPolicy(RoutingPolicyEntity entity) {
        return new RoutingPolicy(
                entity.getId(),
                entity.getPolicyCode(),
                MessageCategory.valueOf(entity.getCategory()),
                MessagePriority.valueOf(entity.getPriorityThreshold()),
                fromChannelJson(entity.getTargetChannelOrder()),
                fromChannelJson(entity.getFallbackChannelOrder()),
                QuietWindowBehavior.valueOf(entity.getQuietWindowBehavior()),
                entity.getDedupWindowSeconds(),
                entity.getEscalationPolicy(),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private DeliveryTaskEntity toDeliveryTaskEntity(DeliveryTask task) {
        DeliveryTaskEntity entity = new DeliveryTaskEntity();
        entity.setId(task.id());
        entity.setNotificationId(task.notificationId());
        entity.setChannelType(task.channelType().name());
        entity.setEndpointId(task.endpointId());
        entity.setRouteOrder(task.routeOrder());
        entity.setStatus(task.status().name());
        entity.setRetryCount(task.retryCount());
        entity.setNextRetryAt(task.nextRetryAt());
        entity.setProviderMessageId(task.providerMessageId());
        entity.setLastErrorCode(task.lastErrorCode());
        entity.setLastErrorMessage(task.lastErrorMessage());
        entity.setDeliveredAt(task.deliveredAt());
        entity.setTenantId(task.tenantId());
        entity.setCreatedAt(task.createdAt());
        entity.setUpdatedAt(task.updatedAt());
        return entity;
    }

    private DeliveryTask toDeliveryTask(DeliveryTaskEntity entity, List<DeliveryAttempt> attempts) {
        return new DeliveryTask(
                entity.getId(),
                entity.getNotificationId(),
                ChannelType.valueOf(entity.getChannelType()),
                entity.getEndpointId(),
                entity.getRouteOrder(),
                DeliveryTaskStatus.valueOf(entity.getStatus()),
                entity.getRetryCount(),
                entity.getNextRetryAt(),
                entity.getProviderMessageId(),
                entity.getLastErrorCode(),
                entity.getLastErrorMessage(),
                entity.getDeliveredAt(),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                attempts
        );
    }

    private DeliveryAttemptEntity toAttemptEntity(DeliveryAttempt attempt) {
        DeliveryAttemptEntity entity = new DeliveryAttemptEntity();
        entity.setId(attempt.id());
        entity.setDeliveryTaskId(attempt.deliveryTaskId());
        entity.setAttemptNo(attempt.attemptNo());
        entity.setRequestPayloadSnapshot(attempt.requestPayloadSnapshot());
        entity.setProviderResponse(attempt.providerResponse());
        entity.setResultStatus(attempt.resultStatus().name());
        entity.setErrorCode(attempt.errorCode());
        entity.setErrorMessage(attempt.errorMessage());
        entity.setRequestedAt(attempt.requestedAt());
        entity.setCompletedAt(attempt.completedAt());
        return entity;
    }

    private DeliveryAttempt toAttempt(DeliveryAttemptEntity entity) {
        return new DeliveryAttempt(
                entity.getId(),
                entity.getDeliveryTaskId(),
                entity.getAttemptNo(),
                entity.getRequestPayloadSnapshot(),
                entity.getProviderResponse(),
                DeliveryAttemptResultStatus.valueOf(entity.getResultStatus()),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getRequestedAt(),
                entity.getCompletedAt()
        );
    }

    private String toChannelJson(List<ChannelType> channels) {
        return channels.stream().map(channel -> "\"" + channel.name() + "\"").collect(java.util.stream.Collectors
                .joining(",", "[", "]"));
    }

    private List<ChannelType> fromChannelJson(String value) {
        if (value == null || value.isBlank() || "[]".equals(value.trim())) {
            return List.of();
        }
        String content = value.trim().replace("[", "").replace("]", "").replace("\"", "");
        if (content.isBlank()) {
            return List.of();
        }
        return Arrays.stream(content.split(",")).map(String::trim).map(ChannelType::valueOf).toList();
    }

    private String normalizeLocale(String locale) {
        return locale.trim().replace('_', '-').toLowerCase(java.util.Locale.ROOT);
    }

    private <T> LambdaQueryWrapper<T> tenantCriteria(
            LambdaQueryWrapper<T> wrapper,
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, UUID> column,
            UUID tenantId
    ) {
        if (tenantId == null) {
            return wrapper.isNull(column);
        }
        return wrapper.eq(column, tenantId);
    }
}
