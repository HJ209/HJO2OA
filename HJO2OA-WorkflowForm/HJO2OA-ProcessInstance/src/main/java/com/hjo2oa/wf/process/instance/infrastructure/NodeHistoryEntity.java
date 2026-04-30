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
@TableName("proc_node_history")
public class NodeHistoryEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("instance_id")
    private UUID instanceId;

    @TableField("task_id")
    private UUID taskId;

    @TableField("node_id")
    private String nodeId;

    @TableField("node_name")
    private String nodeName;

    @TableField("node_type")
    private String nodeType;

    @TableField("history_status")
    private String historyStatus;

    @TableField("action_code")
    private String actionCode;

    @TableField("operator_id")
    private UUID operatorId;

    @TableField("occurred_at")
    private Instant occurredAt;

    @TableField("tenant_id")
    private UUID tenantId;
}
