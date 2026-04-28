package com.hjo2oa.org.identity.context.interfaces.dto;

import java.time.Instant;

public record LoginResponse(
        String token,
        String tokenType,
        Instant expiresAt
) {
}
