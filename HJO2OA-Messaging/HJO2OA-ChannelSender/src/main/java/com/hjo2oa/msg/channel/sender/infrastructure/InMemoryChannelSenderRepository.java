package com.hjo2oa.msg.channel.sender.infrastructure;

import com.hjo2oa.msg.channel.sender.domain.ChannelEndpoint;
import com.hjo2oa.msg.channel.sender.domain.ChannelSenderRepository;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import com.hjo2oa.msg.channel.sender.domain.DeliveryTask;
import com.hjo2oa.msg.channel.sender.domain.DeliveryTaskStatus;
import com.hjo2oa.msg.channel.sender.domain.MessageCategory;
import com.hjo2oa.msg.channel.sender.domain.MessagePriority;
import com.hjo2oa.msg.channel.sender.domain.MessageTemplate;
import com.hjo2oa.msg.channel.sender.domain.RoutingPolicy;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryChannelSenderRepository implements ChannelSenderRepository {

    private final Map<UUID, MessageTemplate> templates = new LinkedHashMap<>();
    private final Map<UUID, ChannelEndpoint> endpoints = new LinkedHashMap<>();
    private final Map<UUID, RoutingPolicy> routingPolicies = new LinkedHashMap<>();
    private final Map<UUID, DeliveryTask> deliveryTasks = new LinkedHashMap<>();

    @Override
    public MessageTemplate saveTemplate(MessageTemplate template) {
        templates.put(template.id(), Objects.requireNonNull(template, "template must not be null"));
        return template;
    }

    @Override
    public Optional<MessageTemplate> findTemplateById(UUID templateId) {
        return Optional.ofNullable(templates.get(templateId));
    }

    @Override
    public Optional<MessageTemplate> findTemplate(
            UUID tenantId,
            String code,
            ChannelType channelType,
            String locale,
            int version
    ) {
        String normalizedLocale = locale.trim().replace('_', '-').toLowerCase(java.util.Locale.ROOT);
        return templates.values().stream()
                .filter(template -> Objects.equals(template.tenantId(), tenantId))
                .filter(template -> template.code().equals(code))
                .filter(template -> template.channelType() == channelType)
                .filter(template -> template.locale().equals(normalizedLocale))
                .filter(template -> template.version() == version)
                .findFirst();
    }

    @Override
    public List<MessageTemplate> findTemplates(UUID tenantId, MessageCategory category) {
        return templates.values().stream()
                .filter(template -> Objects.equals(template.tenantId(), tenantId))
                .filter(template -> category == null || template.category() == category)
                .toList();
    }

    @Override
    public ChannelEndpoint saveEndpoint(ChannelEndpoint endpoint) {
        endpoints.put(endpoint.id(), Objects.requireNonNull(endpoint, "endpoint must not be null"));
        return endpoint;
    }

    @Override
    public Optional<ChannelEndpoint> findEndpointById(UUID endpointId) {
        return Optional.ofNullable(endpoints.get(endpointId));
    }

    @Override
    public Optional<ChannelEndpoint> findEndpoint(UUID tenantId, String endpointCode) {
        return endpoints.values().stream()
                .filter(endpoint -> Objects.equals(endpoint.tenantId(), tenantId))
                .filter(endpoint -> endpoint.endpointCode().equals(endpointCode))
                .findFirst();
    }

    @Override
    public List<ChannelEndpoint> findEndpoints(UUID tenantId, ChannelType channelType) {
        return endpoints.values().stream()
                .filter(endpoint -> Objects.equals(endpoint.tenantId(), tenantId))
                .filter(endpoint -> channelType == null || endpoint.channelType() == channelType)
                .toList();
    }

    @Override
    public RoutingPolicy saveRoutingPolicy(RoutingPolicy policy) {
        routingPolicies.put(policy.id(), Objects.requireNonNull(policy, "policy must not be null"));
        return policy;
    }

    @Override
    public Optional<RoutingPolicy> findRoutingPolicyById(UUID policyId) {
        return Optional.ofNullable(routingPolicies.get(policyId));
    }

    @Override
    public Optional<RoutingPolicy> findRoutingPolicy(UUID tenantId, String policyCode) {
        return routingPolicies.values().stream()
                .filter(policy -> Objects.equals(policy.tenantId(), tenantId))
                .filter(policy -> policy.policyCode().equals(policyCode))
                .findFirst();
    }

    @Override
    public List<RoutingPolicy> findEnabledRoutingPolicies(
            UUID tenantId,
            MessageCategory category,
            MessagePriority priority
    ) {
        return routingPolicies.values().stream()
                .filter(policy -> Objects.equals(policy.tenantId(), tenantId))
                .filter(RoutingPolicy::enabled)
                .filter(policy -> category == null || policy.category() == category)
                .filter(policy -> priority == null || priority.ordinal() >= policy.priorityThreshold().ordinal())
                .toList();
    }

    @Override
    public DeliveryTask saveDeliveryTask(DeliveryTask task) {
        deliveryTasks.put(task.id(), Objects.requireNonNull(task, "task must not be null"));
        return task;
    }

    @Override
    public Optional<DeliveryTask> findDeliveryTaskById(UUID taskId) {
        return Optional.ofNullable(deliveryTasks.get(taskId));
    }

    @Override
    public List<DeliveryTask> findRetryableTasks(UUID tenantId) {
        Instant now = Instant.now();
        return deliveryTasks.values().stream()
                .filter(task -> task.tenantId().equals(tenantId))
                .filter(task -> task.status() == DeliveryTaskStatus.FAILED)
                .filter(task -> task.nextRetryAt() == null || !task.nextRetryAt().isAfter(now))
                .toList();
    }
}
