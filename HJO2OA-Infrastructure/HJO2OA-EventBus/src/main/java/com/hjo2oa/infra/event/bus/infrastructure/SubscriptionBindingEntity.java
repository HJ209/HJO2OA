package com.hjo2oa.infra.event.bus.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("infra_event_subscription")
public class SubscriptionBindingEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("event_definition_id")
    private UUID eventDefinitionId;

    @TableField("subscriber_code")
    private String subscriberCode;

    @TableField("match_mode")
    private String matchMode;

    @TableField("retry_policy")
    private String retryPolicy;

    @TableField("dead_letter_enabled")
    private Boolean deadLetterEnabled;

    @TableField("active")
    private Boolean active;
}
