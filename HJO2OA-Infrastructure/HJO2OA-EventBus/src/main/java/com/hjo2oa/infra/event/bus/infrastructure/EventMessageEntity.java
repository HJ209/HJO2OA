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
@TableName("infra_event_message")
public class EventMessageEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("event_definition_id")
    private UUID eventDefinitionId;

    @TableField("event_type")
    private String eventType;

    @TableField("source")
    private String source;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("correlation_id")
    private String correlationId;

    @TableField("trace_id")
    private String traceId;

    @TableField("operator_account_id")
    private UUID operatorAccountId;

    @TableField("operator_person_id")
    private UUID operatorPersonId;

    @TableField("payload")
    private String payload;

    @TableField("publish_status")
    private String publishStatus;

    @TableField("published_at")
    private Instant publishedAt;

    @TableField("retained_until")
    private Instant retainedUntil;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
