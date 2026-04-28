package com.hjo2oa.portal.portal.home.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("portal_home_refresh_state")
public class PortalHomeRefreshStateEntity {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("person_id")
    private String personId;

    @TableField("assignment_id")
    private String assignmentId;

    @TableField("scene_type")
    private String sceneType;

    @TableField("status")
    private String status;

    @TableField("trigger_event")
    private String triggerEvent;

    @TableField("card_type")
    private String cardType;

    @TableField("message")
    private String message;

    @TableField("updated_at")
    private Instant updatedAt;
}
