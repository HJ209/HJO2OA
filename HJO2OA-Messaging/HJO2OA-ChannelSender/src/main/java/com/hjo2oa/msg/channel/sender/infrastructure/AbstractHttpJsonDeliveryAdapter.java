package com.hjo2oa.msg.channel.sender.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.msg.channel.sender.application.ChannelDeliveryAdapter;
import com.hjo2oa.msg.channel.sender.application.ChannelDeliveryRequest;
import com.hjo2oa.msg.channel.sender.application.ChannelDeliveryResult;
import com.hjo2oa.msg.channel.sender.domain.ChannelEndpointStatus;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class AbstractHttpJsonDeliveryAdapter implements ChannelDeliveryAdapter {

    private final ChannelType channelType;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    AbstractHttpJsonDeliveryAdapter(ChannelType channelType, ObjectMapper objectMapper) {
        this.channelType = channelType;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public ChannelType channelType() {
        return channelType;
    }

    @Override
    public ChannelDeliveryResult send(ChannelDeliveryRequest request) {
        if (request.endpoint() == null) {
            return ChannelDeliveryResult.failure("MSG_ENDPOINT_UNAVAILABLE", "No enabled endpoint is configured", null);
        }
        if (request.endpoint().status() != ChannelEndpointStatus.ENABLED) {
            return ChannelDeliveryResult.failure("MSG_ENDPOINT_DISABLED", "Endpoint is disabled", null);
        }
        URI uri = toUri(request.endpoint().configRef());
        if (uri == null) {
            return ChannelDeliveryResult.failure(
                    "MSG_ENDPOINT_CONFIG_INVALID",
                    "Endpoint configRef must be an http or https URL",
                    null
            );
        }
        try {
            String payload = objectMapper.writeValueAsString(payload(request));
            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("X-HJO2OA-Channel", channelType.name())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return ChannelDeliveryResult.success(
                        response.headers().firstValue("X-Provider-Message-Id")
                                .orElse(channelType.name().toLowerCase() + ":" + request.deliveryTaskId()),
                        response.body()
                );
            }
            return ChannelDeliveryResult.failure(
                    "MSG_PROVIDER_REJECTED",
                    "Provider returned HTTP " + response.statusCode(),
                    response.body()
            );
        } catch (JsonProcessingException ex) {
            return ChannelDeliveryResult.failure("MSG_PAYLOAD_SERIALIZE_FAILED", ex.getMessage(), null);
        } catch (IOException ex) {
            return ChannelDeliveryResult.failure("MSG_PROVIDER_IO_FAILED", ex.getMessage(), null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ChannelDeliveryResult.failure("MSG_PROVIDER_INTERRUPTED", ex.getMessage(), null);
        }
    }

    private Map<String, Object> payload(ChannelDeliveryRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("notificationId", request.notificationId());
        payload.put("deliveryTaskId", request.deliveryTaskId());
        payload.put("tenantId", request.tenantId());
        payload.put("recipientId", request.recipientId());
        payload.put("title", request.title());
        payload.put("body", request.body());
        payload.put("deepLink", request.deepLink());
        payload.put("attributes", request.attributes());
        return payload;
    }

    private URI toUri(String configRef) {
        if (configRef == null || configRef.isBlank()) {
            return null;
        }
        URI uri = URI.create(configRef.trim());
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return null;
        }
        return uri;
    }
}
