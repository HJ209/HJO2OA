package com.hjo2oa.biz.collaboration.hub.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_collab_idempotency_record")
public class IdempotencyRecordEntity {

    @TableId(value = "idempotency_id", type = IdType.ASSIGN_UUID)
    private String idempotencyId;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("operation_code")
    private String operationCode;
    @TableField("idempotency_key")
    private String idempotencyKey;
    @TableField("resource_type")
    private String resourceType;
    @TableField("resource_id")
    private String resourceId;
    @TableField("created_at")
    private Instant createdAt;
}
