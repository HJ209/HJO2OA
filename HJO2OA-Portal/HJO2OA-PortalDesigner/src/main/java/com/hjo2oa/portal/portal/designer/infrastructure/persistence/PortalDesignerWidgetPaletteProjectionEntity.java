package com.hjo2oa.portal.portal.designer.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("portal_designer_widget_palette_projection")
public class PortalDesignerWidgetPaletteProjectionEntity {

    @TableId(value = "widget_id", type = IdType.ASSIGN_UUID)
    private String widgetId;

    @TableField("widget_code")
    private String widgetCode;

    @TableField("card_type")
    private String cardType;

    @TableField("scene_type")
    private String sceneType;

    @TableField("state")
    private String state;

    @TableField("changed_fields_json")
    private String changedFieldsJson;

    @TableField("trigger_event_type")
    private String triggerEventType;

    @TableField("updated_at")
    private Instant updatedAt;
}
