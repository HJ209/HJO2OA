package com.hjo2oa.data.openapi.domain;

import java.time.Instant;
import java.util.List;

public record OpenApiEndpointView(
        String apiId,
        String code,
        String name,
        String path,
        OpenApiHttpMethod httpMethod,
        String version,
        OpenApiAuthType authType,
        OpenApiStatus status,
        String dataServiceId,
        String dataServiceCode,
        String dataServiceName,
        String compatibilityNotes,
        Instant publishedAt,
        Instant deprecatedAt,
        Instant sunsetAt,
        Instant createdAt,
        Instant updatedAt,
        List<ApiCredentialGrant> credentialGrants,
        List<ApiRateLimitPolicy> rateLimitPolicies,
        List<OpenApiVersionRelationView> versions,
        OpenApiInvocationSummary invocationSummary,
        String recentAlertSummary
) {

    public OpenApiEndpointView {
        credentialGrants = credentialGrants == null ? List.of() : List.copyOf(credentialGrants);
        rateLimitPolicies = rateLimitPolicies == null ? List.of() : List.copyOf(rateLimitPolicies);
        versions = versions == null ? List.of() : List.copyOf(versions);
    }
}
