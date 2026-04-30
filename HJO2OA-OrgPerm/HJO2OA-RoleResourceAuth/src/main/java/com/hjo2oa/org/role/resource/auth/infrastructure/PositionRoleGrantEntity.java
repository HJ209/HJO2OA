package com.hjo2oa.org.role.resource.auth.infrastructure;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("org_position_role")
public class PositionRoleGrantEntity {

    @TableId
    private UUID id;
    private UUID positionId;
    private UUID roleId;
    private UUID tenantId;
    private Instant createdAt;
}
