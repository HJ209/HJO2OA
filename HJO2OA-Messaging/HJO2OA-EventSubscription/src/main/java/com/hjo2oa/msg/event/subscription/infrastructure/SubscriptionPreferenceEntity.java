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
@TableName("msg_subscription_preference")
public class SubscriptionPreferenceEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("person_id")
    private UUID personId;

    @TableField("category")
    private String category;

    @TableField("allowed_channels")
    private String allowedChannels;

    @TableField("quiet_window")
    private String quietWindow;

    @TableField("digest_mode")
    private String digestMode;

    @TableField("escalation_opt_in")
    private Boolean escalationOptIn;

    @TableField("mute_non_working_hours")
    private Boolean muteNonWorkingHours;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
