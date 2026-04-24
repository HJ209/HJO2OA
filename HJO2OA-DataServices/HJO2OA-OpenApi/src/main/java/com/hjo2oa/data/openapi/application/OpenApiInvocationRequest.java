package com.hjo2oa.data.openapi.application;

import java.util.Map;

public record OpenApiInvocationRequest(
        Map<String, String> queryParameters,
        String requestBody
) {

    public OpenApiInvocationRequest {
        queryParameters = queryParameters == null ? Map.of() : Map.copyOf(queryParameters);
    }
}
