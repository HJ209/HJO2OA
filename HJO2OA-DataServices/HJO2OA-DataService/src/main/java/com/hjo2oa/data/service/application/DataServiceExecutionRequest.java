package com.hjo2oa.data.service.application;

import com.hjo2oa.data.service.domain.DataServiceDefinition;
import java.util.Map;
import java.util.Objects;

public record DataServiceExecutionRequest(
        DataServiceDefinition definition,
        String requestId,
        String clientCode,
        Map<String, String> queryParameters,
        String requestBody
) {

    public DataServiceExecutionRequest {
        Objects.requireNonNull(definition, "definition must not be null");
        requestId = requireText(requestId, "requestId");
        clientCode = requireText(clientCode, "clientCode");
        queryParameters = queryParameters == null ? Map.of() : Map.copyOf(queryParameters);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
