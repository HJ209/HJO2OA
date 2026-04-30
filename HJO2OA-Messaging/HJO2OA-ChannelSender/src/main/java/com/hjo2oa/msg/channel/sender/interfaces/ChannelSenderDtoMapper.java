package com.hjo2oa.msg.channel.sender.interfaces;

import com.hjo2oa.msg.channel.sender.application.RenderedMessageView;
import com.hjo2oa.msg.channel.sender.domain.ChannelEndpointView;
import com.hjo2oa.msg.channel.sender.domain.DeliveryAttemptView;
import com.hjo2oa.msg.channel.sender.domain.DeliveryTaskView;
import com.hjo2oa.msg.channel.sender.domain.MessageTemplateView;
import com.hjo2oa.msg.channel.sender.domain.RoutingPolicyView;
import org.springframework.stereotype.Component;

@Component
public class ChannelSenderDtoMapper {

    public ChannelSenderDtos.TemplateResponse toTemplateResponse(MessageTemplateView view) {
        return new ChannelSenderDtos.TemplateResponse(
                view.id(),
                view.code(),
                view.channelType(),
                view.locale(),
                view.version(),
                view.category(),
                view.titleTemplate(),
                view.bodyTemplate(),
                view.variableSchema(),
                view.status(),
                view.systemLocked(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public ChannelSenderDtos.RenderTemplateResponse toRenderTemplateResponse(RenderedMessageView view) {
        return new ChannelSenderDtos.RenderTemplateResponse(
                view.templateId(),
                view.templateCode(),
                view.channelType(),
                view.locale(),
                view.version(),
                view.title(),
                view.body()
        );
    }

    public ChannelSenderDtos.EndpointResponse toEndpointResponse(ChannelEndpointView view) {
        return new ChannelSenderDtos.EndpointResponse(
                view.id(),
                view.endpointCode(),
                view.channelType(),
                view.providerType(),
                view.displayName(),
                view.status(),
                view.configRef(),
                view.rateLimitPerMinute(),
                view.dailyQuota(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public ChannelSenderDtos.RoutingPolicyResponse toRoutingPolicyResponse(RoutingPolicyView view) {
        return new ChannelSenderDtos.RoutingPolicyResponse(
                view.id(),
                view.policyCode(),
                view.category(),
                view.priorityThreshold(),
                view.targetChannelOrder(),
                view.fallbackChannelOrder(),
                view.quietWindowBehavior(),
                view.dedupWindowSeconds(),
                view.escalationPolicy(),
                view.enabled(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public ChannelSenderDtos.DeliveryTaskResponse toDeliveryTaskResponse(DeliveryTaskView view) {
        return new ChannelSenderDtos.DeliveryTaskResponse(
                view.id(),
                view.notificationId(),
                view.channelType(),
                view.endpointId(),
                view.routeOrder(),
                view.status(),
                view.retryCount(),
                view.nextRetryAt(),
                view.providerMessageId(),
                view.lastErrorCode(),
                view.lastErrorMessage(),
                view.deliveredAt(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt(),
                view.attempts().stream().map(this::toAttemptResponse).toList()
        );
    }

    private ChannelSenderDtos.DeliveryAttemptResponse toAttemptResponse(DeliveryAttemptView view) {
        return new ChannelSenderDtos.DeliveryAttemptResponse(
                view.id(),
                view.deliveryTaskId(),
                view.attemptNo(),
                view.requestPayloadSnapshot(),
                view.providerResponse(),
                view.resultStatus(),
                view.errorCode(),
                view.errorMessage(),
                view.requestedAt(),
                view.completedAt()
        );
    }
}
