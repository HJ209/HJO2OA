package com.hjo2oa.data.openapi.interfaces;

import com.hjo2oa.data.openapi.domain.ApiPolicyType;
import com.hjo2oa.data.openapi.domain.ApiWindowUnit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpsertApiRateLimitPolicyRequest(
        String clientCode,
        @NotNull ApiPolicyType policyType,
        @Positive long windowValue,
        @NotNull ApiWindowUnit windowUnit,
        @Positive long threshold,
        String description
) {
}
