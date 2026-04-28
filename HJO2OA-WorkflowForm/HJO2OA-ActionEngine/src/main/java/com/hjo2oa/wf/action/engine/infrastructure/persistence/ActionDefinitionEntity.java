package com.hjo2oa.wf.action.engine.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("wf_action_engine_definition")
public class ActionDefinitionEntity {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
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

    @TableField("ui_config_json")
    private String uiConfigJson;

    @TableField("tenant_id")
    private String tenantId;
}
