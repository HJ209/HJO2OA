package com.hjo2oa.msg.mobile.support.infrastructure;

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
@TableName("msg_mobile_session")
public class MobileSessionEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("device_binding_id")
    private UUID deviceBindingId;

    @TableField("person_id")
    private UUID personId;

    @TableField("account_id")
    private UUID accountId;

    @TableField("current_assignment_id")
    private UUID currentAssignmentId;

    @TableField("current_position_id")
    private UUID currentPositionId;

    @TableField("session_status")
    private String sessionStatus;

    @TableField("risk_level_snapshot")
    private String riskLevelSnapshot;

    @TableField("risk_frozen_at")
    private Instant riskFrozenAt;

    @TableField("risk_reason")
    private String riskReason;

    @TableField("issued_at")
    private Instant issuedAt;

    @TableField("expires_at")
    private Instant expiresAt;

    @TableField("last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @TableField("refresh_version")
    private Integer refreshVersion;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
