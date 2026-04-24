package com.hjo2oa.data.openapi.domain;

public enum ApiInvocationOutcome {
    SUCCESS,
    AUTH_FAILED,
    RATE_LIMITED,
    QUOTA_EXCEEDED,
    DEPENDENCY_ERROR,
    ERROR
}
