package com.hjo2oa.msg.channel.sender.infrastructure;

import com.hjo2oa.msg.channel.sender.application.ChannelDeliveryAdapter;
import com.hjo2oa.msg.channel.sender.application.ChannelDeliveryRequest;
import com.hjo2oa.msg.channel.sender.application.ChannelDeliveryResult;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import org.springframework.stereotype.Component;

@Component
public class InboxDeliveryAdapter implements ChannelDeliveryAdapter {

    @Override
    public ChannelType channelType() {
        return ChannelType.INBOX;
    }

    @Override
    public ChannelDeliveryResult send(ChannelDeliveryRequest request) {
        return ChannelDeliveryResult.success(
                "inbox:" + request.notificationId(),
                "{\"channel\":\"INBOX\",\"status\":\"DELIVERED\"}"
        );
    }
}
