package com.hjo2oa.msg.message.center.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("msg_notification")
public class NotificationEntity {

    @TableId(value = "notification_id", type = IdType.ASSIGN_UUID)
    private String notificationId;

    @TableField("dedup_key")
    private String dedupKey;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("recipient_id")
    private String recipientId;

    @TableField("target_assignment_id")
    private String targetAssignmentId;

    @TableField("target_position_id")
    private String targetPositionId;

    @TableField("title")
    private String title;

    @TableField("body_summary")
    private String bodySummary;

    @TableField("deep_link")
    private String deepLink;

    @TableField("category")
    private String category;

    @TableField("priority")
    private String priority;

    @TableField("inbox_status")
    private String inboxStatus;

    @TableField("source_module")
    private String sourceModule;

    @TableField("source_event_type")
    private String sourceEventType;

    @TableField("source_business_id")
    private String sourceBusinessId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    @TableField("read_at")
    private Instant readAt;

    @TableField("archived_at")
    private Instant archivedAt;

    @TableField("revoked_at")
    private Instant revokedAt;

    @TableField("expired_at")
    private Instant expiredAt;

    @TableField("status_reason")
    private String statusReason;
}
