package com.hjo2oa.todo.center.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("wf_copied_todo_item")
public class CopiedTodoEntity {

    @TableId(value = "todo_id", type = IdType.ASSIGN_UUID)
    private String todoId;

    @TableField("task_id")
    private String taskId;

    @TableField("instance_id")
    private String instanceId;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("recipient_assignment_id")
    private String recipientAssignmentId;

    @TableField("todo_type")
    private String type;

    @TableField("category")
    private String category;

    @TableField("title")
    private String title;

    @TableField("urgency")
    private String urgency;

    @TableField("read_status")
    private String readStatus;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    @TableField("read_at")
    private Instant readAt;
}
