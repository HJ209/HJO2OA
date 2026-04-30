package com.hjo2oa.shared.web;

public record SharedRequestContext(
        String tenantId,
        String requestId,
        String language,
        String timezone,
        String idempotencyKey
) {

    public static final String ATTRIBUTE_NAME = SharedRequestContext.class.getName();
}
