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
@TableName("msg_device_binding")
public class DeviceBindingEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("person_id")
    private UUID personId;

    @TableField("account_id")
    private UUID accountId;

    @TableField("device_id")
    private String deviceId;

    @TableField("device_fingerprint")
    private String deviceFingerprint;

    @TableField("platform")
    private String platform;

    @TableField("app_type")
    private String appType;

    @TableField("push_token")
    private String pushToken;

    @TableField("bind_status")
    private String bindStatus;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("last_login_at")
    private Instant lastLoginAt;

    @TableField("last_seen_at")
    private Instant lastSeenAt;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
