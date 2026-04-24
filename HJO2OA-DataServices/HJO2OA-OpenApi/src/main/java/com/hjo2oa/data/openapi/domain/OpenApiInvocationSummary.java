package com.hjo2oa.data.openapi.domain;

import java.time.Instant;

public record OpenApiInvocationSummary(
        long totalCalls,
        long successCalls,
        Instant lastOccurredAt
) {
}
