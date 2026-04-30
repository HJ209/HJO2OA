package com.hjo2oa.infra.scheduler.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hjo2oa.infra.scheduler.domain.ExecutionStatus;
import com.hjo2oa.infra.scheduler.domain.TriggerSource;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("infra_job_execution_record")
public class JobExecutionRecordEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String scheduledJobId;
    private String parentExecutionId;
    private TriggerSource triggerSource;
    private ExecutionStatus executionStatus;
    private Instant startedAt;
    private Instant finishedAt;
    private Long durationMs;
    private Integer attemptNo;
    private Integer maxAttempts;
    private String errorCode;
    private String errorMessage;
    private String errorStack;
    private String executionLog;
    private String triggerContext;
    private String idempotencyKey;
    private Instant nextRetryAt;
}
