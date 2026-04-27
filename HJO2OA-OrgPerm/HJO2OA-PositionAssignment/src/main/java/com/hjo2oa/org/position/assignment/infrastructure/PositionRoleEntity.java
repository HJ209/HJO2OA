package com.hjo2oa.org.position.assignment.infrastructure;

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
@TableName("org_position_role")
public class PositionRoleEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("position_id")
    private UUID positionId;

    @TableField("role_id")
    private UUID roleId;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;
}
