package com.hjo2oa.data.service.application;

import java.util.Map;

public record DataServiceExecutionResult(
        Map<String, Object> payload
) {

    public DataServiceExecutionResult {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
