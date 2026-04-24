package com.hjo2oa.data.service.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.data.common.domain.exception.DataServicesErrorCode;
import com.hjo2oa.data.common.domain.exception.DataServicesException;
import org.springframework.stereotype.Component;

@Component
public class DataServiceJsonCodec {

    private final ObjectMapper objectMapper;

    public DataServiceJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_INTERNAL_ERROR,
                    "Failed to serialize data service JSON payload",
                    exception
            );
        }
    }

    public <T> T read(String value, Class<T> type) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_INTERNAL_ERROR,
                    "Failed to deserialize data service JSON payload",
                    exception
            );
        }
    }
}
