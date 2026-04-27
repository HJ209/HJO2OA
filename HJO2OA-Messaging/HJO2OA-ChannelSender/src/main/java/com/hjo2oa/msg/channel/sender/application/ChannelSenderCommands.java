package com.hjo2oa.msg.channel.sender.application;

import com.hjo2oa.msg.channel.sender.domain.ChannelEndpointStatus;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import com.hjo2oa.msg.channel.sender.domain.DeliveryAttemptResultStatus;
import com.hjo2oa.msg.channel.sender.domain.MessageCategory;
import com.hjo2oa.msg.channel.sender.domain.MessagePriority;
import com.hjo2oa.msg.channel.sender.domain.ProviderType;
import com.hjo2oa.msg.channel.sender.domain.QuietWindowBehavior;
import java.util.List;
import java.util.UUID;

public final class ChannelSenderCommands {

    private ChannelSenderCommands() {
    }

    public record CreateTemplateCommand(
            String code,
            ChannelType channelType,
            String locale,
            Integer version,
            MessageCategory category,
            String titleTemplate,
            String bodyTemplate,
            String variableSchema,
            boolean systemLocked,
            UUID tenantId
    ) {
    }

    public record CreateEndpointCommand(
            String endpointCode,
            ChannelType channelType,
            ProviderType providerType,
            String displayName,
            String configRef,
            Integer rateLimitPerMinute,
            Integer dailyQuota,
            UUID tenantId
    ) {
    }

    public record ChangeEndpointStatusCommand(UUID endpointId, ChannelEndpointStatus status) {
    }

    public record CreateRoutingPolicyCommand(
            String policyCode,
            MessageCategory category,
            MessagePriority priorityThreshold,
            List<ChannelType> targetChannelOrder,
            List<ChannelType> fallbackChannelOrder,
            QuietWindowBehavior quietWindowBehavior,
            Integer dedupWindowSeconds,
            String escalationPolicy,
            UUID tenantId
    ) {
    }

    public record CreateDeliveryTaskCommand(
            UUID notificationId,
            ChannelType channelType,
            UUID endpointId,
            int routeOrder,
            UUID tenantId
    ) {
    }

    public record RouteDeliveryTasksCommand(
            UUID notificationId,
            MessageCategory category,
            MessagePriority priority,
            UUID tenantId
    ) {
    }

    public record RecordDeliveryResultCommand(
            UUID deliveryTaskId,
            DeliveryAttemptResultStatus resultStatus,
            String requestPayloadSnapshot,
            String providerResponse,
            String providerMessageId,
            String errorCode,
            String errorMessage
    ) {
    }
}
