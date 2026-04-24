package com.hjo2oa.data.openapi.domain;

import java.time.Instant;

public record OpenApiEndpointListItemView(
        String apiId,
        String code,
        String name,
        String path,
        OpenApiHttpMethod httpMethod,
        String version,
        OpenApiAuthType authType,
        OpenApiStatus status,
        String dataServiceCode,
        String dataServiceName,
        OpenApiInvocationSummary invocationSummary,
        String recentAlertSummary,
        Instant publishedAt,
        Instant deprecatedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
