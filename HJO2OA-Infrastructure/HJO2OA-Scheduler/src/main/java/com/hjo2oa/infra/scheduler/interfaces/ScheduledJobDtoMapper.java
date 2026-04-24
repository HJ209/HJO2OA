package com.hjo2oa.infra.scheduler.interfaces;

import com.hjo2oa.infra.scheduler.application.ScheduledJobCommands;
import com.hjo2oa.infra.scheduler.domain.JobExecutionRecordView;
import com.hjo2oa.infra.scheduler.domain.ScheduledJobView;
import org.springframework.stereotype.Component;

@Component
public class ScheduledJobDtoMapper {

    public ScheduledJobCommands.RegisterJobCommand toRegisterCommand(ScheduledJobDtos.RegisterJobRequest request) {
        return new ScheduledJobCommands.RegisterJobCommand(
                request.jobCode(),
                request.name(),
                request.triggerType(),
                request.cronExpr(),
                request.timezoneId(),
                request.concurrencyPolicy(),
                request.timeoutSeconds(),
                request.retryPolicy(),
                request.tenantId()
        );
    }

    public ScheduledJobDtos.ScheduledJobResponse toResponse(ScheduledJobView view) {
        return new ScheduledJobDtos.ScheduledJobResponse(
                view.id(),
                view.jobCode(),
                view.name(),
                view.triggerType(),
                view.cronExpr(),
                view.timezoneId(),
                view.concurrencyPolicy(),
                view.timeoutSeconds(),
                view.retryPolicy(),
                view.status(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public ScheduledJobDtos.JobExecutionRecordResponse toResponse(JobExecutionRecordView view) {
        return new ScheduledJobDtos.JobExecutionRecordResponse(
                view.id(),
                view.scheduledJobId(),
                view.triggerSource(),
                view.executionStatus(),
                view.startedAt(),
                view.finishedAt(),
                view.errorCode(),
                view.errorMessage(),
                view.executionLog()
        );
    }
}
