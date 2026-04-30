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
@TableName("proc_variable_history")
public class VariableHistoryEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("instance_id")
    private UUID instanceId;

    @TableField("task_id")
    private UUID taskId;

    @TableField("variable_name")
    private String variableName;

    @TableField("old_value")
    private String oldValue;

    @TableField("new_value")
    private String newValue;

    @TableField("operator_id")
    private UUID operatorId;

    @TableField("occurred_at")
    private Instant occurredAt;

    @TableField("tenant_id")
    private UUID tenantId;
}
