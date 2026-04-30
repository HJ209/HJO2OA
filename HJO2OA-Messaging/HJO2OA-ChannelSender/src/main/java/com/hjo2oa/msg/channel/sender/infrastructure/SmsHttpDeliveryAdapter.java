package com.hjo2oa.msg.channel.sender.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import org.springframework.stereotype.Component;

@Component
public class SmsHttpDeliveryAdapter extends AbstractHttpJsonDeliveryAdapter {

    public SmsHttpDeliveryAdapter(ObjectMapper objectMapper) {
        super(ChannelType.SMS, objectMapper);
    }
}
