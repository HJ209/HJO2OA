package com.hjo2oa.org.data.permission.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("org_field_permission")
public class FieldPermissionEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("subject_type")
    private String subjectType;

    @TableField("subject_id")
    private UUID subjectId;

    @TableField("business_object")
    private String businessObject;

    @TableField("usage_scenario")
    private String usageScenario;

    @TableField("field_code")
    private String fieldCode;

    @TableField("action")
    private String action;

    @TableField("effect")
    private String effect;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
