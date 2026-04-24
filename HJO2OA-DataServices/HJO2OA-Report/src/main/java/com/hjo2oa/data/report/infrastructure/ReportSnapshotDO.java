package com.hjo2oa.data.report.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("data_report_snapshot")
public class ReportSnapshotDO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("report_id")
    private String reportId;

    @TableField("snapshot_at")
    private Instant snapshotAt;

    @TableField("refresh_batch")
    private String refreshBatch;

    @TableField("scope_signature")
    private String scopeSignature;

    @TableField("payload")
    private String payload;

    @TableField("freshness_status")
    private String freshnessStatus;

    @TableField("trigger_mode")
    private String triggerMode;

    @TableField("trigger_reason")
    private String triggerReason;

    @TableField("error_message")
    private String errorMessage;

    @TableField("deleted")
    private Boolean deleted;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
