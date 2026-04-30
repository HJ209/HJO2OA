package com.hjo2oa.infra.scheduler.application;

public interface SchedulerJobHandler {

    String handlerName();

    SchedulerJobResult execute(SchedulerJobExecutionContext context) throws Exception;
}
