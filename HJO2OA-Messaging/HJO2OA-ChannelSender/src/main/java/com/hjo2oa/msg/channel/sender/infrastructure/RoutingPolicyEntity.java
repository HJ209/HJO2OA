package com.hjo2oa.msg.channel.sender.infrastructure;

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
@TableName("msg_routing_policy")
public class RoutingPolicyEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("policy_code")
    private String policyCode;

    @TableField("category")
    private String category;

    @TableField("priority_threshold")
    private String priorityThreshold;

    @TableField("target_channel_order")
    private String targetChannelOrder;

    @TableField("fallback_channel_order")
    private String fallbackChannelOrder;

    @TableField("quiet_window_behavior")
    private String quietWindowBehavior;

    @TableField("dedup_window_seconds")
    private Integer dedupWindowSeconds;

    @TableField("escalation_policy")
    private String escalationPolicy;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
