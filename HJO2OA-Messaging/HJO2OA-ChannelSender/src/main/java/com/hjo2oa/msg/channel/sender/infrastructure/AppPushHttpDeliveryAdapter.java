package com.hjo2oa.msg.channel.sender.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import org.springframework.stereotype.Component;

@Component
public class AppPushHttpDeliveryAdapter extends AbstractHttpJsonDeliveryAdapter {

    public AppPushHttpDeliveryAdapter(ObjectMapper objectMapper) {
        super(ChannelType.APP_PUSH, objectMapper);
    }
}
