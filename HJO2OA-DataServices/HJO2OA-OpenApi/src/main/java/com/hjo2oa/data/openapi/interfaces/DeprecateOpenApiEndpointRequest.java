package com.hjo2oa.data.openapi.interfaces;

import java.time.Instant;

public record DeprecateOpenApiEndpointRequest(
        Instant sunsetAt
) {
}
