package com.hjo2oa.msg.channel.sender.application;

import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import java.util.UUID;

public record RenderedMessageView(
        UUID templateId,
        String templateCode,
        ChannelType channelType,
        String locale,
        int version,
        String title,
        String body
) {
}
