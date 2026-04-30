package com.hjo2oa.infra.scheduler.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hjo2oa.infra.scheduler.domain.ConcurrencyPolicy;
import com.hjo2oa.infra.scheduler.domain.JobStatus;
import com.hjo2oa.infra.scheduler.domain.TriggerType;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("infra_scheduled_job")
public class ScheduledJobEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String jobCode;
    private String handlerName;
    private String name;
    private TriggerType triggerType;
    private String cronExpr;
    private String timezoneId;
    private ConcurrencyPolicy concurrencyPolicy;
    private Integer timeoutSeconds;
    private String retryPolicy;
    private JobStatus status;
    private String tenantId;
    private Instant createdAt;
    private Instant updatedAt;
}
