package com.hjo2oa.org.role.resource.auth.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("org_resource_permission")
public class ResourcePermissionEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("role_id")
    private UUID roleId;

    @TableField("resource_type")
    private String resourceType;

    @TableField("resource_code")
    private String resourceCode;

    @TableField("action")
    private String action;

    @TableField("effect")
    private String effect;

    @TableField("tenant_id")
    private UUID tenantId;
}
