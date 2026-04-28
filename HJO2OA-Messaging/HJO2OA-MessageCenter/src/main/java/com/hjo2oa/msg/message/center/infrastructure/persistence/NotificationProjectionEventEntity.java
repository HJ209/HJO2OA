package com.hjo2oa.msg.message.center.infrastructure.persistence;

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
@TableName("msg_notification_projection_event")
public class NotificationProjectionEventEntity {

    @TableId(value = "event_id", type = IdType.ASSIGN_UUID)
    private UUID eventId;

    @TableField("processed_at")
    private Instant processedAt;
}
