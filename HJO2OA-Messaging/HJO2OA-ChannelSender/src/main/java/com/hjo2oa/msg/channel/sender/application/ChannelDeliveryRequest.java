package com.hjo2oa.msg.channel.sender.application;

import com.hjo2oa.msg.channel.sender.domain.ChannelEndpoint;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import java.util.Map;
import java.util.UUID;

public record ChannelDeliveryRequest(
        UUID deliveryTaskId,
        UUID notificationId,
        UUID tenantId,
        ChannelType channelType,
        ChannelEndpoint endpoint,
        String recipientId,
        String title,
        String body,
        String deepLink,
        Map<String, Object> attributes
) {

    public ChannelDeliveryRequest {
        attributes = attributes == null
                ? Map.of()
                : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(attributes));
    }
}
