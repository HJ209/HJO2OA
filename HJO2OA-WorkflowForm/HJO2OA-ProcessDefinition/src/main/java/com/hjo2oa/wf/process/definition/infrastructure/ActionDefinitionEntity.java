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
@TableName("proc_action_def")
public class ActionDefinitionEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("category")
    private String category;

    @TableField("route_target")
    private String routeTarget;

    @TableField("require_opinion")
    private Boolean requireOpinion;

    @TableField("require_target")
    private Boolean requireTarget;

    @TableField("ui_config")
    private String uiConfig;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
