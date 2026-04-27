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
@TableName("org_sync_diff_record")
public class DiffRecordEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("task_id")
    private UUID taskId;

    @TableField("entity_type")
    private String entityType;

    @TableField("entity_key")
    private String entityKey;

    @TableField("diff_type")
    private String diffType;

    @TableField("status")
    private String status;

    @TableField("source_snapshot")
    private String sourceSnapshot;

    @TableField("local_snapshot")
    private String localSnapshot;

    @TableField("suggestion")
    private String suggestion;

    @TableField("resolved_by")
    private UUID resolvedBy;

    @TableField("resolved_at")
    private Instant resolvedAt;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
