package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
@TableName("event_outbox")
public class EventOutboxEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("event_id")
    private UUID eventId;

    @TableField("aggregate_type")
    private String aggregateType;

    @TableField("aggregate_id")
    private String aggregateId;

    @TableField("event_type")
    private String eventType;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("occurred_at")
    private Instant occurredAt;

    @TableField("trace_id")
    private String traceId;

    @TableField("schema_version")
    private String schemaVersion;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private Instant createdAt;

    @TableField(value = "published_at", updateStrategy = FieldStrategy.ALWAYS)
    private Instant publishedAt;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField(value = "next_retry_at", updateStrategy = FieldStrategy.ALWAYS)
    private Instant nextRetryAt;

    @TableField(value = "last_error", updateStrategy = FieldStrategy.ALWAYS)
    private String lastError;

    @TableField(value = "dead_at", updateStrategy = FieldStrategy.ALWAYS)
    private Instant deadAt;
}
