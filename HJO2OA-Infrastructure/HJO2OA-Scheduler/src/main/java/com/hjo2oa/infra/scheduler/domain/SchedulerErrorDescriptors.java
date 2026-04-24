package com.hjo2oa.infra.scheduler.domain;

import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import org.springframework.http.HttpStatus;

public final class SchedulerErrorDescriptors {

    public static final ErrorDescriptor SCHEDULED_JOB_NOT_FOUND =
            SharedErrorDescriptors.of("SCHEDULED_JOB_NOT_FOUND", HttpStatus.NOT_FOUND, "Scheduled job not found");
    public static final ErrorDescriptor JOB_EXECUTION_NOT_FOUND =
            SharedErrorDescriptors.of("JOB_EXECUTION_NOT_FOUND", HttpStatus.NOT_FOUND, "Job execution not found");
    public static final ErrorDescriptor JOB_CODE_CONFLICT =
            SharedErrorDescriptors.of("JOB_CODE_CONFLICT", HttpStatus.CONFLICT, "Scheduled job code already exists");
    public static final ErrorDescriptor JOB_DEFINITION_INVALID =
            SharedErrorDescriptors.of("JOB_DEFINITION_INVALID", HttpStatus.BAD_REQUEST, "Scheduled job definition is invalid");
    public static final ErrorDescriptor JOB_STATE_INVALID =
            SharedErrorDescriptors.of("JOB_STATE_INVALID", HttpStatus.UNPROCESSABLE_ENTITY, "Scheduled job state is invalid");
    public static final ErrorDescriptor EXECUTION_STATE_INVALID =
            SharedErrorDescriptors.of("EXECUTION_STATE_INVALID", HttpStatus.CONFLICT, "Job execution state is invalid");
    public static final ErrorDescriptor EXECUTION_CONFLICT =
            SharedErrorDescriptors.of("EXECUTION_CONFLICT", HttpStatus.CONFLICT, "Scheduled job is already running");
    public static final ErrorDescriptor TRIGGER_SOURCE_INVALID =
            SharedErrorDescriptors.of("TRIGGER_SOURCE_INVALID", HttpStatus.UNPROCESSABLE_ENTITY, "Trigger source is invalid");

    private SchedulerErrorDescriptors() {
    }
}
