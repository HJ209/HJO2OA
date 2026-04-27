package com.hjo2oa.org.role.resource.auth.infrastructure;

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
@TableName("org_person_role")
public class PersonRoleEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("person_id")
    private UUID personId;

    @TableField("role_id")
    private UUID roleId;

    @TableField("reason")
    private String reason;

    @TableField("expires_at")
    private Instant expiresAt;

    @TableField("tenant_id")
    private UUID tenantId;
}
