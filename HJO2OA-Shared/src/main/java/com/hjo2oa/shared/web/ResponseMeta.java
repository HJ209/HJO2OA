package com.hjo2oa.shared.web;

import java.time.Instant;

public record ResponseMeta(
        String requestId,
        Instant timestamp,
        String serverTimezone
) {
}
