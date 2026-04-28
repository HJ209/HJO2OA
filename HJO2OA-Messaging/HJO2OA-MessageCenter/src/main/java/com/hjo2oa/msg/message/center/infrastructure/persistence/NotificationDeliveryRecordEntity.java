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
@TableName("msg_notification_delivery_record")
public class NotificationDeliveryRecordEntity {

    @TableId(value = "delivery_id", type = IdType.ASSIGN_UUID)
    private String deliveryId;

    @TableField("notification_id")
    private String notificationId;

    @TableField("channel")
    private String channel;

    @TableField("status")
    private String status;

    @TableField("attempt_count")
    private Integer attemptCount;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    @TableField("delivered_at")
    private Instant deliveredAt;

    @TableField("last_error_code")
    private String lastErrorCode;
}
