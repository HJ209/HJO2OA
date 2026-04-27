package com.hjo2oa.msg.event.subscription.infrastructure;

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
@TableName("msg_subscription_rule")
public class SubscriptionRuleEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("rule_code")
    private String ruleCode;

    @TableField("event_type_pattern")
    private String eventTypePattern;

    @TableField("notification_category")
    private String notificationCategory;

    @TableField("target_resolver_type")
    private String targetResolverType;

    @TableField("target_resolver_config")
    private String targetResolverConfig;

    @TableField("template_code")
    private String templateCode;

    @TableField("condition_expr")
    private String conditionExpr;

    @TableField("priority_mapping")
    private String priorityMapping;

    @TableField("default_priority")
    private String defaultPriority;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
