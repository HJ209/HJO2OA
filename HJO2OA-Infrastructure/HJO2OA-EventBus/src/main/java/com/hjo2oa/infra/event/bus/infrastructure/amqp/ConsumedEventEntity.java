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
@TableName("consumed_event")
public class ConsumedEventEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("event_id")
    private UUID eventId;

    @TableField("event_type")
    private String eventType;

    @TableField("consumer_code")
    private String consumerCode;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("trace_id")
    private String traceId;

    @TableField("status")
    private String status;

    @TableField(value = "last_error", updateStrategy = FieldStrategy.ALWAYS)
    private String lastError;

    @TableField("consumed_at")
    private Instant consumedAt;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
