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
@TableName("org_audit_record")
public class AuditRecordEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("category")
    private String category;

    @TableField("action_type")
    private String actionType;

    @TableField("entity_type")
    private String entityType;

    @TableField("entity_id")
    private String entityId;

    @TableField("task_id")
    private UUID taskId;

    @TableField("trigger_source")
    private String triggerSource;

    @TableField("operator_id")
    private UUID operatorId;

    @TableField("before_snapshot")
    private String beforeSnapshot;

    @TableField("after_snapshot")
    private String afterSnapshot;

    @TableField("summary")
    private String summary;

    @TableField("occurred_at")
    private Instant occurredAt;

    @TableField("created_at")
    private Instant createdAt;
}
