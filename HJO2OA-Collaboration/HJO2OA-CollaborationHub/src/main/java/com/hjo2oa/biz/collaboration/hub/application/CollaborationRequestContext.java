package com.hjo2oa.biz.collaboration.hub.application;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public record CollaborationRequestContext(
        String tenantId,
        String actorId,
        String requestId,
        String idempotencyKey,
        String language,
        String timezone
) {

    public static CollaborationRequestContext from(HttpServletRequest request) {
        return new CollaborationRequestContext(
                headerOrDefault(request, "X-Tenant-Id", "tenant-1"),
                firstHeaderOrDefault(
                        request,
                        "assignment-1",
                        "X-Operator-Person-Id",
                        "X-Person-Id",
                        "X-Identity-Person-Id",
                        "X-Identity-Assignment-Id"
                ),
                headerOrDefault(request, "X-Request-Id", UUID.randomUUID().toString()),
                headerOrDefault(request, "X-Idempotency-Key", null),
                headerOrDefault(request, "Accept-Language", "zh-CN"),
                headerOrDefault(request, "X-Timezone", "UTC")
        );
    }

    private static String firstHeaderOrDefault(HttpServletRequest request, String defaultValue, String... names) {
        if (request == null) {
            return defaultValue;
        }
        for (String name : names) {
            String value = request.getHeader(name);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return defaultValue;
    }

    private static String headerOrDefault(HttpServletRequest request, String name, String defaultValue) {
        if (request == null) {
            return defaultValue;
        }
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
