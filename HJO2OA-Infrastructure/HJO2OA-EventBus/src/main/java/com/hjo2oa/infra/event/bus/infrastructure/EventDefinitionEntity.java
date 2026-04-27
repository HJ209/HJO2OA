package com.hjo2oa.infra.event.bus.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("infra_event_definition")
public class EventDefinitionEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("event_type")
    private String eventType;

    @TableField("module_prefix")
    private String modulePrefix;

    @TableField("version")
    private String version;

    @TableField("payload_schema")
    private String payloadSchema;

    @TableField("description")
    private String description;

    @TableField("publish_mode")
    private String publishMode;

    @TableField("status")
    private String status;

    @TableField("owner_module")
    private String ownerModule;

    @TableField("tenant_scope")
    private String tenantScope;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
