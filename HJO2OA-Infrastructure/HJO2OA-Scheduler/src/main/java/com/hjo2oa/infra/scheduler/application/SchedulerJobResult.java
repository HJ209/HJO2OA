package com.hjo2oa.infra.scheduler.application;

public record SchedulerJobResult(String executionLog) {

    public SchedulerJobResult {
        if (executionLog != null) {
            executionLog = executionLog.trim();
            if (executionLog.isEmpty()) {
                executionLog = null;
            }
        }
    }

    public static SchedulerJobResult success(String executionLog) {
        return new SchedulerJobResult(executionLog);
    }

    public static SchedulerJobResult success() {
        return new SchedulerJobResult(null);
    }
}
