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
@TableName("infra_secret_key_material")
public class SecretKeyMaterialEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("security_policy_id")
    private String securityPolicyId;

    @TableField("key_ref")
    private String keyRef;

    @TableField("algorithm")
    private String algorithm;

    @TableField("key_status")
    private String keyStatus;

    @TableField("rotated_at")
    private Instant rotatedAt;
}
