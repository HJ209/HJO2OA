package com.hjo2oa.data.openapi.interfaces;

public record ReviewApiInvocationAuditLogRequest(
        boolean abnormalFlag,
        String reviewConclusion,
        String note
) {
}
