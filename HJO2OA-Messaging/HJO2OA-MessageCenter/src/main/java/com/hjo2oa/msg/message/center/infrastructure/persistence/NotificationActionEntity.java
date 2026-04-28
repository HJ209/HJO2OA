package com.hjo2oa.msg.message.center.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("msg_notification_action")
public class NotificationActionEntity {

    @TableId(value = "action_id", type = IdType.ASSIGN_UUID)
    private String actionId;

    @TableField("notification_id")
    private String notificationId;

    @TableField("action_type")
    private String actionType;

    @TableField("operator_id")
    private String operatorId;

    @TableField("occurred_at")
    private Instant occurredAt;
}
