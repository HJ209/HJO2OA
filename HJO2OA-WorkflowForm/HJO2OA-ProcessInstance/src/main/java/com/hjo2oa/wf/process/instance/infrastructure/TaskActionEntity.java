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
@TableName("proc_task_action")
public class TaskActionEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("task_id")
    private UUID taskId;

    @TableField("instance_id")
    private UUID instanceId;

    @TableField("action_code")
    private String actionCode;

    @TableField("action_name")
    private String actionName;

    @TableField("operator_id")
    private UUID operatorId;

    @TableField("operator_org_id")
    private UUID operatorOrgId;

    @TableField("operator_position_id")
    private UUID operatorPositionId;

    @TableField("opinion")
    private String opinion;

    @TableField("target_node_id")
    private String targetNodeId;

    @TableField("form_data_patch")
    private String formDataPatch;

    @TableField("created_at")
    private Instant createdAt;
}
