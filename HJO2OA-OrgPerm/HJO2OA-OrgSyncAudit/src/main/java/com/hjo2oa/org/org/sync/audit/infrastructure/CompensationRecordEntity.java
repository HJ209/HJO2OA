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
@TableName("org_sync_compensation_record")
public class CompensationRecordEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("task_id")
    private UUID taskId;

    @TableField("diff_record_id")
    private UUID diffRecordId;

    @TableField("action_type")
    private String actionType;

    @TableField("status")
    private String status;

    @TableField("request_payload")
    private String requestPayload;

    @TableField("result_payload")
    private String resultPayload;

    @TableField("operator_id")
    private UUID operatorId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
