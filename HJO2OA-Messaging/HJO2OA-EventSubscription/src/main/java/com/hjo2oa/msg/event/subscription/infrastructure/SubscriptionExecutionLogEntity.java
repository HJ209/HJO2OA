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
@TableName("msg_subscription_execution_log")
public class SubscriptionExecutionLogEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("event_id")
    private UUID eventId;

    @TableField("event_type")
    private String eventType;

    @TableField("rule_code")
    private String ruleCode;

    @TableField("recipient_id")
    private String recipientId;

    @TableField("result")
    private String result;

    @TableField("message")
    private String message;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("occurred_at")
    private Instant occurredAt;
}
