package com.hjo2oa.org.identity.context.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank String token
) {
}
