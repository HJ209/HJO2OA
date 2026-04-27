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
@TableName("org_sync_source_config")
public class SyncSourceConfigEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("source_code")
    private String sourceCode;

    @TableField("source_name")
    private String sourceName;

    @TableField("source_type")
    private String sourceType;

    @TableField("endpoint")
    private String endpoint;

    @TableField("config_ref")
    private String configRef;

    @TableField("scope_expression")
    private String scopeExpression;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
