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
@TableName("biz_collab_task")
public class TaskEntity {

    @TableId(value = "task_id", type = IdType.ASSIGN_UUID)
    private String taskId;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("workspace_id")
    private String workspaceId;
    @TableField("title")
    private String title;
    @TableField("description")
    private String description;
    @TableField("creator_id")
    private String creatorId;
    @TableField("assignee_id")
    private String assigneeId;
    @TableField("status")
    private String status;
    @TableField("priority")
    private String priority;
    @TableField("due_at")
    private Instant dueAt;
    @TableField("completed_at")
    private Instant completedAt;
    @TableField("created_at")
    private Instant createdAt;
    @TableField("updated_at")
    private Instant updatedAt;
}
