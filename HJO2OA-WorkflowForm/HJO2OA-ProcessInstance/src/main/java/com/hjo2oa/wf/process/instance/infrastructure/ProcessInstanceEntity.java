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
@TableName("proc_instance")
public class ProcessInstanceEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("definition_id")
    private UUID definitionId;

    @TableField("definition_version")
    private Integer definitionVersion;

    @TableField("definition_code")
    private String definitionCode;

    @TableField("title")
    private String title;

    @TableField("category")
    private String category;

    @TableField("initiator_id")
    private UUID initiatorId;

    @TableField("initiator_org_id")
    private UUID initiatorOrgId;

    @TableField("initiator_dept_id")
    private UUID initiatorDeptId;

    @TableField("initiator_position_id")
    private UUID initiatorPositionId;

    @TableField("form_metadata_id")
    private UUID formMetadataId;

    @TableField("form_data_id")
    private UUID formDataId;

    @TableField("current_nodes")
    private String currentNodes;

    @TableField("status")
    private String status;

    @TableField("start_time")
    private Instant startTime;

    @TableField("end_time")
    private Instant endTime;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
