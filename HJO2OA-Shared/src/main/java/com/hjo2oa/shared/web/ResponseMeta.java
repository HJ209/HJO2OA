package com.hjo2oa.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseMeta(
        String requestId,
        Instant timestamp,
        String serverTimezone,
        String tenantId,
        String language,
        String timezone,
        String idempotencyKey
) {
}
