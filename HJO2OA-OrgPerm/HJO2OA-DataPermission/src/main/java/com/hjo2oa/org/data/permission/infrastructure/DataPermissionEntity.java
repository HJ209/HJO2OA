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
@TableName("org_data_permission")
public class DataPermissionEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("subject_type")
    private String subjectType;

    @TableField("subject_id")
    private UUID subjectId;

    @TableField("business_object")
    private String businessObject;

    @TableField("scope_type")
    private String scopeType;

    @TableField("condition_expr")
    private String conditionExpr;

    @TableField("effect")
    private String effect;

    @TableField("priority")
    private Integer priority;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
