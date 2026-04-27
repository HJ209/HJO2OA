package com.hjo2oa.wf.process.definition.infrastructure;

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
@TableName("proc_definition")
public class ProcessDefinitionEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("category")
    private String category;

    @TableField("version")
    private Integer version;

    @TableField("status")
    private String status;

    @TableField("form_metadata_id")
    private UUID formMetadataId;

    @TableField("start_node_id")
    private String startNodeId;

    @TableField("end_node_id")
    private String endNodeId;

    @TableField("nodes")
    private String nodes;

    @TableField("routes")
    private String routes;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("published_at")
    private Instant publishedAt;

    @TableField("published_by")
    private UUID publishedBy;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
