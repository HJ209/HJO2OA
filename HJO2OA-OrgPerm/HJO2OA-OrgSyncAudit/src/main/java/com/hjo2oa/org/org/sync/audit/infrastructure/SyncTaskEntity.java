package com.hjo2oa.org.org.sync.audit.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("org_sync_task")
public class SyncTaskEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("source_id")
    private UUID sourceId;

    @TableField("task_type")
    private String taskType;

    @TableField("status")
    private String status;

    @TableField("retry_of_task_id")
    private UUID retryOfTaskId;

    @TableField("trigger_source")
    private String triggerSource;

    @TableField("operator_id")
    private UUID operatorId;

    @TableField("started_at")
    private Instant startedAt;

    @TableField("finished_at")
    private Instant finishedAt;

    @TableField("success_count")
    private Integer successCount;

    @TableField("failure_count")
    private Integer failureCount;

    @TableField("diff_count")
    private Integer diffCount;

    @TableField("failure_reason")
    private String failureReason;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
