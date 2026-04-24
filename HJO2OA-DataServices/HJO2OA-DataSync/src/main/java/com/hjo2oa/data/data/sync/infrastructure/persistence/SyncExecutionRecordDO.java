package com.hjo2oa.data.data.sync.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hjo2oa.data.common.infrastructure.persistence.BaseEntityDO;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("data_sync_execution_record")
public class SyncExecutionRecordDO extends BaseEntityDO {

    @TableField("sync_task_id")
    private UUID syncTaskId;

    @TableField("task_code")
    private String taskCode;

    @TableField("parent_execution_id")
    private UUID parentExecutionId;

    @TableField("execution_batch_no")
    private String executionBatchNo;

    @TableField("trigger_type")
    private String triggerType;

    @TableField("execution_status")
    private String executionStatus;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("checkpoint_value")
    private String checkpointValue;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("retryable")
    private Boolean retryable;

    @TableField("result_summary_json")
    private String resultSummaryJson;

    @TableField("diff_summary_json")
    private String diffSummaryJson;

    @TableField("difference_details_json")
    private String differenceDetailsJson;

    @TableField("trigger_context_json")
    private String triggerContextJson;

    @TableField("failure_code")
    private String failureCode;

    @TableField("failure_message")
    private String failureMessage;

    @TableField("reconciliation_status")
    private String reconciliationStatus;

    @TableField("operator_account_id")
    private String operatorAccountId;

    @TableField("operator_person_id")
    private String operatorPersonId;

    @TableField("started_at")
    private Instant startedAt;

    @TableField("finished_at")
    private Instant finishedAt;
}
