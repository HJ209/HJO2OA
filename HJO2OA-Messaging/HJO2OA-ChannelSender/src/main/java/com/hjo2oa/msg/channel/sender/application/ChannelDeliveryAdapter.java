package com.hjo2oa.msg.channel.sender.application;

import com.hjo2oa.msg.channel.sender.domain.ChannelType;

public interface ChannelDeliveryAdapter {

    ChannelType channelType();

    ChannelDeliveryResult send(ChannelDeliveryRequest request);
}
