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
@TableName("msg_delivery_attempt")
public class DeliveryAttemptEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("delivery_task_id")
    private UUID deliveryTaskId;

    @TableField("attempt_no")
    private Integer attemptNo;

    @TableField("request_payload_snapshot")
    private String requestPayloadSnapshot;

    @TableField("provider_response")
    private String providerResponse;

    @TableField("result_status")
    private String resultStatus;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("requested_at")
    private Instant requestedAt;

    @TableField("completed_at")
    private Instant completedAt;
}
