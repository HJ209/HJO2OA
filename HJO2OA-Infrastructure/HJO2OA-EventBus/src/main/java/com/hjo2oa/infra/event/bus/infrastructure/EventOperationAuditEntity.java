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
@TableName("infra_event_operation_audit")
public class EventOperationAuditEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("event_id")
    private UUID eventId;

    @TableField("operation_type")
    private String operationType;

    @TableField("operator_account_id")
    private UUID operatorAccountId;

    @TableField("operator_person_id")
    private UUID operatorPersonId;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("trace_id")
    private String traceId;

    @TableField("request_id")
    private String requestId;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("reason")
    private String reason;

    @TableField("detail_json")
    private String detailJson;

    @TableField("created_at")
    private Instant createdAt;
}
