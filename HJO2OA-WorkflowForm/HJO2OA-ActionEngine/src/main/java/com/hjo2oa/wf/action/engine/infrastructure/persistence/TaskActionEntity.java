package com.hjo2oa.wf.action.engine.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("wf_action_engine_task_action")
public class TaskActionEntity {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("task_id")
    private UUID taskId;

    @TableField("instance_id")
    private UUID instanceId;

    @TableField("action_code")
    private String actionCode;

    @TableField("category")
    private String category;

    @TableField("opinion")
    private String opinion;

    @TableField("target_node_id")
    private String targetNodeId;

    @TableField("target_assignee_ids_json")
    private String targetAssigneeIdsJson;

    @TableField("form_data_patch_json")
    private String formDataPatchJson;

    @TableField("operator_account_id")
    private String operatorAccountId;

    @TableField("operator_person_id")
    private String operatorPersonId;

    @TableField("operator_position_id")
    private String operatorPositionId;

    @TableField("operator_org_id")
    private String operatorOrgId;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("result_status")
    private String resultStatus;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("tenant_id")
    private String tenantId;
}
