package com.hjo2oa.wf.process.instance.domain;

public enum TaskNodeType {
    START,
    END,
    USER_TASK,
    SERVICE_TASK,
    EXCLUSIVE_GATEWAY,
    PARALLEL_GATEWAY,
    INCLUSIVE_GATEWAY,
    SUB_PROCESS
}
