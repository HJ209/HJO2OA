package com.hjo2oa.data.connector.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.data.connector.domain.TimeoutRetryConfig;
import org.springframework.stereotype.Component;

@Component
public class ConnectorJsonCodec {

    private final ObjectMapper objectMapper;

    public ConnectorJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String writeTimeoutConfig(TimeoutRetryConfig timeoutRetryConfig) {
        try {
            return objectMapper.writeValueAsString(timeoutRetryConfig);
        } catch (JsonProcessingException ex) {
            throw new BizException(SharedErrorDescriptors.INTERNAL_ERROR, "连接器超时配置序列化失败", ex);
        }
    }

    public TimeoutRetryConfig readTimeoutConfig(String timeoutConfig) {
        if (timeoutConfig == null || timeoutConfig.isBlank()) {
            return TimeoutRetryConfig.defaultConfig();
        }
        try {
            return objectMapper.readValue(timeoutConfig, TimeoutRetryConfig.class);
        } catch (JsonProcessingException ex) {
            throw new BizException(SharedErrorDescriptors.INTERNAL_ERROR, "连接器超时配置反序列化失败", ex);
        }
    }
}
