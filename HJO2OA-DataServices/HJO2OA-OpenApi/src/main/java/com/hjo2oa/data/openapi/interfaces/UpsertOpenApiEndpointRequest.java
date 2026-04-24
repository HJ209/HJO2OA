package com.hjo2oa.data.openapi.interfaces;

import com.hjo2oa.data.openapi.domain.OpenApiAuthType;
import com.hjo2oa.data.openapi.domain.OpenApiHttpMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertOpenApiEndpointRequest(
        @NotBlank String name,
        @NotBlank String dataServiceCode,
        @NotBlank String path,
        @NotNull OpenApiHttpMethod httpMethod,
        @NotNull OpenApiAuthType authType,
        String compatibilityNotes
) {
}
