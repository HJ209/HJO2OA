package com.hjo2oa.infra.scheduler.domain;

public enum TriggerSource {
    CRON,
    MANUAL,
    RETRY,
    DEPENDENCY
}
