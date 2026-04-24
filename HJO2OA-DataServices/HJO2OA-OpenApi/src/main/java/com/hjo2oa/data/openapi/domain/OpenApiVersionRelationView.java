package com.hjo2oa.data.openapi.domain;

import java.time.Instant;

public record OpenApiVersionRelationView(
        String version,
        OpenApiStatus status,
        Instant publishedAt,
        Instant deprecatedAt,
        Instant sunsetAt
) {
}
