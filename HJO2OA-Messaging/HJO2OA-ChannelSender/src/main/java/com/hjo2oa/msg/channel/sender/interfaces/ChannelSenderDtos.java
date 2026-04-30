package com.hjo2oa.msg.channel.sender.interfaces;

import com.hjo2oa.msg.channel.sender.application.ChannelSenderCommands;
import com.hjo2oa.msg.channel.sender.domain.ChannelEndpointStatus;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import com.hjo2oa.msg.channel.sender.domain.DeliveryAttemptResultStatus;
import com.hjo2oa.msg.channel.sender.domain.DeliveryTaskStatus;
import com.hjo2oa.msg.channel.sender.domain.MessageCategory;
import com.hjo2oa.msg.channel.sender.domain.MessagePriority;
import com.hjo2oa.msg.channel.sender.domain.MessageTemplateStatus;
import com.hjo2oa.msg.channel.sender.domain.ProviderType;
import com.hjo2oa.msg.channel.sender.domain.QuietWindowBehavior;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ChannelSenderDtos {

    private ChannelSenderDtos() {
    }

    public record CreateTemplateRequest(
            @NotBlank @Size(max = 64) String code,
            @NotNull ChannelType channelType,
            @NotBlank @Size(max = 16) String locale,
            @Min(1) Integer version,
            @NotNull MessageCategory category,
            @NotBlank @Size(max = 256) String titleTemplate,
            @NotBlank @Size(max = 4000) String bodyTemplate,
            @Size(max = 4000) String variableSchema,
            boolean systemLocked,
            UUID tenantId
    ) {

        public ChannelSenderCommands.CreateTemplateCommand toCommand() {
            return new ChannelSenderCommands.CreateTemplateCommand(
                    code,
                    channelType,
                    locale,
                    version,
                    category,
                    titleTemplate,
                    bodyTemplate,
                    variableSchema,
                    systemLocked,
                    tenantId
            );
        }
    }

    public record TemplateResponse(
            UUID id,
            String code,
            ChannelType channelType,
            String locale,
            int version,
            MessageCategory category,
            String titleTemplate,
            String bodyTemplate,
            String variableSchema,
            MessageTemplateStatus status,
            boolean systemLocked,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record RenderTemplateRequest(
            UUID tenantId,
            @NotBlank @Size(max = 64) String templateCode,
            @NotNull ChannelType channelType,
            @NotBlank @Size(max = 16) String locale,
            Map<String, Object> variables
    ) {

        public ChannelSenderCommands.RenderTemplateCommand toCommand() {
            return new ChannelSenderCommands.RenderTemplateCommand(
                    tenantId,
                    templateCode,
                    channelType,
                    locale,
                    variables
            );
        }
    }

    public record RenderTemplateResponse(
            UUID templateId,
            String templateCode,
            ChannelType channelType,
            String locale,
            int version,
            String title,
            String body
    ) {
    }

    public record CreateEndpointRequest(
            @NotBlank @Size(max = 64) String endpointCode,
            @NotNull ChannelType channelType,
            @NotNull ProviderType providerType,
            @NotBlank @Size(max = 128) String displayName,
            @NotBlank @Size(max = 128) String configRef,
            @Min(0) Integer rateLimitPerMinute,
            @Min(0) Integer dailyQuota,
            UUID tenantId
    ) {

        public ChannelSenderCommands.CreateEndpointCommand toCommand() {
            return new ChannelSenderCommands.CreateEndpointCommand(
                    endpointCode,
                    channelType,
                    providerType,
                    displayName,
                    configRef,
                    rateLimitPerMinute,
                    dailyQuota,
                    tenantId
            );
        }
    }

    public record ChangeEndpointStatusRequest(@NotNull ChannelEndpointStatus status) {
    }

    public record EndpointResponse(
            UUID id,
            String endpointCode,
            ChannelType channelType,
            ProviderType providerType,
            String displayName,
            ChannelEndpointStatus status,
            String configRef,
            Integer rateLimitPerMinute,
            Integer dailyQuota,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record SendTestRequest(
            @NotNull UUID tenantId,
            @NotNull ChannelType channelType,
            @NotNull UUID endpointId,
            @NotBlank @Size(max = 512) String target,
            @NotBlank @Size(max = 256) String title,
            @NotBlank @Size(max = 4000) String body,
            @Size(max = 512) String deepLink
    ) {

        public ChannelSenderCommands.SendTestCommand toCommand() {
            return new ChannelSenderCommands.SendTestCommand(
                    tenantId,
                    channelType,
                    endpointId,
                    target,
                    title,
                    body,
                    deepLink
            );
        }
    }

    public record CreateRoutingPolicyRequest(
            @NotBlank @Size(max = 64) String policyCode,
            @NotNull MessageCategory category,
            @NotNull MessagePriority priorityThreshold,
            @NotNull List<ChannelType> targetChannelOrder,
            List<ChannelType> fallbackChannelOrder,
            @NotNull QuietWindowBehavior quietWindowBehavior,
            @Min(0) Integer dedupWindowSeconds,
            @Size(max = 4000) String escalationPolicy,
            UUID tenantId
    ) {

        public ChannelSenderCommands.CreateRoutingPolicyCommand toCommand() {
            return new ChannelSenderCommands.CreateRoutingPolicyCommand(
                    policyCode,
                    category,
                    priorityThreshold,
                    targetChannelOrder,
                    fallbackChannelOrder,
                    quietWindowBehavior,
                    dedupWindowSeconds,
                    escalationPolicy,
                    tenantId
            );
        }
    }

    public record RoutingPolicyResponse(
            UUID id,
            String policyCode,
            MessageCategory category,
            MessagePriority priorityThreshold,
            List<ChannelType> targetChannelOrder,
            List<ChannelType> fallbackChannelOrder,
            QuietWindowBehavior quietWindowBehavior,
            int dedupWindowSeconds,
            String escalationPolicy,
            boolean enabled,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CreateDeliveryTaskRequest(
            @NotNull UUID notificationId,
            @NotNull ChannelType channelType,
            UUID endpointId,
            @Min(0) int routeOrder,
            @NotNull UUID tenantId
    ) {

        public ChannelSenderCommands.CreateDeliveryTaskCommand toCommand() {
            return new ChannelSenderCommands.CreateDeliveryTaskCommand(
                    notificationId,
                    channelType,
                    endpointId,
                    routeOrder,
                    tenantId
            );
        }
    }

    public record RouteDeliveryTasksRequest(
            @NotNull UUID notificationId,
            @NotNull MessageCategory category,
            @NotNull MessagePriority priority,
            @NotNull UUID tenantId
    ) {

        public ChannelSenderCommands.RouteDeliveryTasksCommand toCommand() {
            return new ChannelSenderCommands.RouteDeliveryTasksCommand(notificationId, category, priority, tenantId);
        }
    }

    public record RecordDeliveryResultRequest(
            @NotNull DeliveryAttemptResultStatus resultStatus,
            @Size(max = 4000) String requestPayloadSnapshot,
            @Size(max = 4000) String providerResponse,
            @Size(max = 128) String providerMessageId,
            @Size(max = 64) String errorCode,
            @Size(max = 512) String errorMessage
    ) {

        public ChannelSenderCommands.RecordDeliveryResultCommand toCommand(UUID deliveryTaskId) {
            return new ChannelSenderCommands.RecordDeliveryResultCommand(
                    deliveryTaskId,
                    resultStatus,
                    requestPayloadSnapshot,
                    providerResponse,
                    providerMessageId,
                    errorCode,
                    errorMessage
            );
        }
    }

    public record DeliveryAttemptResponse(
            UUID id,
            UUID deliveryTaskId,
            int attemptNo,
            String requestPayloadSnapshot,
            String providerResponse,
            DeliveryAttemptResultStatus resultStatus,
            String errorCode,
            String errorMessage,
            Instant requestedAt,
            Instant completedAt
    ) {
    }

    public record DeliveryTaskResponse(
            UUID id,
            UUID notificationId,
            ChannelType channelType,
            UUID endpointId,
            int routeOrder,
            DeliveryTaskStatus status,
            int retryCount,
            Instant nextRetryAt,
            String providerMessageId,
            String lastErrorCode,
            String lastErrorMessage,
            Instant deliveredAt,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt,
            List<DeliveryAttemptResponse> attempts
    ) {
    }
}
