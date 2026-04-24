package com.hjo2oa.infra.security.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("infra_masking_rule")
public class MaskingRuleEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("security_policy_id")
    private String securityPolicyId;

    @TableField("data_type")
    private String dataType;

    @TableField("rule_expr")
    private String ruleExpr;

    @TableField("active")
    private Boolean active;
}
