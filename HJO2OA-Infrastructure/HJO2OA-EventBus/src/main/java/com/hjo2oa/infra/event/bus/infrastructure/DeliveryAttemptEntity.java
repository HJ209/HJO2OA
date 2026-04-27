package com.hjo2oa.infra.event.bus.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("infra_event_delivery_attempt")
public class DeliveryAttemptEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("event_message_id")
    private UUID eventMessageId;

    @TableField("subscriber_code")
    private String subscriberCode;

    @TableField("attempt_no")
    private Integer attemptNo;

    @TableField("delivery_status")
    private String deliveryStatus;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("delivered_at")
    private Instant deliveredAt;

    @TableField("next_retry_at")
    private Instant nextRetryAt;

    @TableField("request_snapshot")
    private String requestSnapshot;

    @TableField("response_snapshot")
    private String responseSnapshot;
}
