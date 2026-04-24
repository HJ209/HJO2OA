package com.hjo2oa.data.data.sync.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hjo2oa.data.common.infrastructure.persistence.BaseEntityDO;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("data_sync_task")
public class SyncExchangeTaskDO extends BaseEntityDO {

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("task_type")
    private String taskType;

    @TableField("sync_mode")
    private String syncMode;

    @TableField("source_connector_id")
    private UUID sourceConnectorId;

    @TableField("target_connector_id")
    private UUID targetConnectorId;

    @TableField("dependency_status")
    private String dependencyStatus;

    @TableField("checkpoint_mode")
    private String checkpointMode;

    @TableField("checkpoint_config_json")
    private String checkpointConfigJson;

    @TableField("trigger_config_json")
    private String triggerConfigJson;

    @TableField("retry_policy_json")
    private String retryPolicyJson;

    @TableField("compensation_policy_json")
    private String compensationPolicyJson;

    @TableField("reconciliation_policy_json")
    private String reconciliationPolicyJson;

    @TableField("schedule_config_json")
    private String scheduleConfigJson;

    @TableField("status")
    private String status;
}
