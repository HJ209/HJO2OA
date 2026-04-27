package com.hjo2oa.msg.ecosystem.infrastructure;

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
@TableName("msg_ecosystem_integration")
public class EcosystemIntegrationEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("integration_type")
    private String integrationType;

    @TableField("display_name")
    private String displayName;

    @TableField("auth_mode")
    private String authMode;

    @TableField("callback_url")
    private String callbackUrl;

    @TableField("sign_algorithm")
    private String signAlgorithm;

    @TableField("config_ref")
    private String configRef;

    @TableField("status")
    private String status;

    @TableField("health_status")
    private String healthStatus;

    @TableField("last_check_at")
    private Instant lastCheckAt;

    @TableField("last_error_summary")
    private String lastErrorSummary;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
