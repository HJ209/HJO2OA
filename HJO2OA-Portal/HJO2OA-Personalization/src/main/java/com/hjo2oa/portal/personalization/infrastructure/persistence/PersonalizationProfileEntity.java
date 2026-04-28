package com.hjo2oa.portal.personalization.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("portal_personalization_profile")
public class PersonalizationProfileEntity {

    @TableId(value = "profile_id", type = IdType.ASSIGN_UUID)
    private String profileId;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("person_id")
    private String personId;

    @TableField("assignment_id")
    private String assignmentId;

    @TableField("scene_type")
    private String sceneType;

    @TableField("base_publication_id")
    private String basePublicationId;

    @TableField("theme_code")
    private String themeCode;

    @TableField("widget_order_json")
    private String widgetOrderJson;

    @TableField("hidden_placement_json")
    private String hiddenPlacementJson;

    @TableField("quick_access_json")
    private String quickAccessJson;

    @TableField("status")
    private String status;

    @TableField("last_resolved_at")
    private Instant lastResolvedAt;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
