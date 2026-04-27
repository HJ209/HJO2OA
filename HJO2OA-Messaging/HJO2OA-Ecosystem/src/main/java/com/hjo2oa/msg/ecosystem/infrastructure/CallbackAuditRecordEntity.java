package com.hjo2oa.msg.ecosystem.infrastructure;

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
@TableName("msg_callback_audit")
public class CallbackAuditRecordEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("integration_id")
    private UUID integrationId;

    @TableField("callback_type")
    private String callbackType;

    @TableField("verify_result")
    private String verifyResult;

    @TableField("payload_summary")
    private String payloadSummary;

    @TableField("error_message")
    private String errorMessage;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("payload_digest")
    private String payloadDigest;

    @TableField("occurred_at")
    private Instant occurredAt;
}
