package com.hjo2oa.infra.scheduler.domain;

public enum ExecutionStatus {
    RUNNING,
    RETRYING,
    SUCCESS,
    FAILED,
    TIMEOUT,
    CANCELLED
}
