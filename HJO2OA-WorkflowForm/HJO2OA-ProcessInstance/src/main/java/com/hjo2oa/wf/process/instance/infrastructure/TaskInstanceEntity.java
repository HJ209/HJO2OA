package com.hjo2oa.wf.process.instance.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("proc_task")
public class TaskInstanceEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("instance_id")
    private UUID instanceId;

    @TableField("node_id")
    private String nodeId;

    @TableField("node_name")
    private String nodeName;

    @TableField("node_type")
    private String nodeType;

    @TableField("assignee_id")
    private UUID assigneeId;

    @TableField("assignee_org_id")
    private UUID assigneeOrgId;

    @TableField("assignee_dept_id")
    private UUID assigneeDeptId;

    @TableField("assignee_position_id")
    private UUID assigneePositionId;

    @TableField("candidate_type")
    private String candidateType;

    @TableField("candidate_ids")
    private String candidateIds;

    @TableField("multi_instance_type")
    private String multiInstanceType;

    @TableField("completion_condition")
    private String completionCondition;

    @TableField("status")
    private String status;

    @TableField("claim_time")
    private Instant claimTime;

    @TableField("completed_time")
    private Instant completedTime;

    @TableField("due_time")
    private Instant dueTime;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
