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
@TableName("msg_delivery_task")
public class DeliveryTaskEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("notification_id")
    private UUID notificationId;

    @TableField("channel_type")
    private String channelType;

    @TableField("endpoint_id")
    private UUID endpointId;

    @TableField("route_order")
    private Integer routeOrder;

    @TableField("status")
    private String status;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("next_retry_at")
    private Instant nextRetryAt;

    @TableField("provider_message_id")
    private String providerMessageId;

    @TableField("last_error_code")
    private String lastErrorCode;

    @TableField("last_error_message")
    private String lastErrorMessage;

    @TableField("delivered_at")
    private Instant deliveredAt;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
