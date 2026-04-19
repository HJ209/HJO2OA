package com.hjo2oa.org.identity.context.interfaces;

import jakarta.validation.constraints.NotBlank;

public record SwitchIdentityContextRequest(
        @NotBlank String targetPositionId,
        String reason
) {
}
