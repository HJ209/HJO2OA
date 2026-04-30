package com.hjo2oa.msg.channel.sender.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import org.springframework.stereotype.Component;

@Component
public class WebhookDeliveryAdapter extends AbstractHttpJsonDeliveryAdapter {

    public WebhookDeliveryAdapter(ObjectMapper objectMapper) {
        super(ChannelType.WEBHOOK, objectMapper);
    }
}
