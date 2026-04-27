package com.hjo2oa.msg.channel.sender.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelSenderRepository {

    MessageTemplate saveTemplate(MessageTemplate template);

    Optional<MessageTemplate> findTemplateById(UUID templateId);

    Optional<MessageTemplate> findTemplate(
            UUID tenantId,
            String code,
            ChannelType channelType,
            String locale,
            int version
    );

    List<MessageTemplate> findTemplates(UUID tenantId, MessageCategory category);

    ChannelEndpoint saveEndpoint(ChannelEndpoint endpoint);

    Optional<ChannelEndpoint> findEndpointById(UUID endpointId);

    Optional<ChannelEndpoint> findEndpoint(UUID tenantId, String endpointCode);

    List<ChannelEndpoint> findEndpoints(UUID tenantId, ChannelType channelType);

    RoutingPolicy saveRoutingPolicy(RoutingPolicy policy);

    Optional<RoutingPolicy> findRoutingPolicyById(UUID policyId);

    Optional<RoutingPolicy> findRoutingPolicy(UUID tenantId, String policyCode);

    List<RoutingPolicy> findEnabledRoutingPolicies(UUID tenantId, MessageCategory category, MessagePriority priority);

    DeliveryTask saveDeliveryTask(DeliveryTask task);

    Optional<DeliveryTask> findDeliveryTaskById(UUID taskId);

    List<DeliveryTask> findRetryableTasks(UUID tenantId);
}
