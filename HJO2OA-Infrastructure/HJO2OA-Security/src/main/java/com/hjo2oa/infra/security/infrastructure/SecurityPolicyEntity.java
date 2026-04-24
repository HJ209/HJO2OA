package com.hjo2oa.infra.security.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("infra_security_policy")
public class SecurityPolicyEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("policy_code")
    private String policyCode;

    @TableField("policy_type")
    private String policyType;

    @TableField("name")
    private String name;

    @TableField("status")
    private String status;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("config_snapshot")
    private String configSnapshot;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
