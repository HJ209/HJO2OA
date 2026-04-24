package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.TimeoutRetryConfig;
import java.util.Map;

public record HttpRequestSpec(
        String url,
        String method,
        Map<String, String> headers,
        TimeoutRetryConfig timeoutRetryConfig
) {
}
