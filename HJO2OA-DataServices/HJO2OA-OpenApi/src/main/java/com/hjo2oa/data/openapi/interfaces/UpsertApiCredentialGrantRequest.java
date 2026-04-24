package com.hjo2oa.data.openapi.interfaces;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public record UpsertApiCredentialGrantRequest(
        @NotBlank String secretRef,
        List<String> scopes,
        Instant expiresAt
) {
}
