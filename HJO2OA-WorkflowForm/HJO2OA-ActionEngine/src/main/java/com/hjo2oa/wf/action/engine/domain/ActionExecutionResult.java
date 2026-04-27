package com.hjo2oa.wf.action.engine.domain;

public record ActionExecutionResult(
        TaskAction taskAction,
        TaskStatus taskStatus
) {
}
