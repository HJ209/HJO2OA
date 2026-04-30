package com.hjo2oa.msg.channel.sender.application;

public record ChannelDeliveryResult(
        boolean success,
        String providerMessageId,
        String providerResponse,
        String errorCode,
        String errorMessage
) {

    public static ChannelDeliveryResult success(String providerMessageId, String providerResponse) {
        return new ChannelDeliveryResult(true, providerMessageId, providerResponse, null, null);
    }

    public static ChannelDeliveryResult failure(String errorCode, String errorMessage, String providerResponse) {
        return new ChannelDeliveryResult(false, null, providerResponse, errorCode, errorMessage);
    }
}
