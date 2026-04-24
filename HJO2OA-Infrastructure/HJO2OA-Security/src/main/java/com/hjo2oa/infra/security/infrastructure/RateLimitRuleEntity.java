package com.hjo2oa.infra.security.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("infra_rate_limit_rule")
public class RateLimitRuleEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("security_policy_id")
    private String securityPolicyId;

    @TableField("subject_type")
    private String subjectType;

    @TableField("window_seconds")
    private Integer windowSeconds;

    @TableField("max_requests")
    private Integer maxRequests;

    @TableField("active")
    private Boolean active;
}
