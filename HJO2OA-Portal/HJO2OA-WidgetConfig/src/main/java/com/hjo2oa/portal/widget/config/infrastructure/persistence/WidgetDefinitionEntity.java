package com.hjo2oa.portal.widget.config.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("portal_widget_definition")
public class WidgetDefinitionEntity {

    @TableId(value = "widget_id", type = IdType.ASSIGN_UUID)
    private String widgetId;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("widget_code")
    private String widgetCode;

    @TableField("display_name")
    private String displayName;

    @TableField("card_type")
    private String cardType;

    @TableField("scene_type")
    private String sceneType;

    @TableField("source_module")
    private String sourceModule;

    @TableField("data_source_type")
    private String dataSourceType;

    @TableField("allow_hide")
    private Boolean allowHide;

    @TableField("allow_collapse")
    private Boolean allowCollapse;

    @TableField("max_items")
    private Integer maxItems;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
