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
@TableName("biz_collab_audit_log")
public class AuditRecordEntity {

    @TableId(value = "audit_id", type = IdType.ASSIGN_UUID)
    private String auditId;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("actor_id")
    private String actorId;
    @TableField("action_code")
    private String actionCode;
    @TableField("resource_type")
    private String resourceType;
    @TableField("resource_id")
    private String resourceId;
    @TableField("request_id")
    private String requestId;
    @TableField("detail")
    private String detail;
    @TableField("occurred_at")
    private Instant occurredAt;
}
