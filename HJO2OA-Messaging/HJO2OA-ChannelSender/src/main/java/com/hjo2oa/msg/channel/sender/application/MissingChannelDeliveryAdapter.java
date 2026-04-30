package com.hjo2oa.msg.channel.sender.application;

import com.hjo2oa.msg.channel.sender.domain.ChannelType;

class MissingChannelDeliveryAdapter implements ChannelDeliveryAdapter {

    private final ChannelType channelType;

    MissingChannelDeliveryAdapter(ChannelType channelType) {
        this.channelType = channelType;
    }

    @Override
    public ChannelType channelType() {
        return channelType;
    }

    @Override
    public ChannelDeliveryResult send(ChannelDeliveryRequest request) {
        return ChannelDeliveryResult.failure(
                "MSG_CHANNEL_UNAVAILABLE",
                "No delivery adapter registered for " + channelType,
                null
        );
    }
}
