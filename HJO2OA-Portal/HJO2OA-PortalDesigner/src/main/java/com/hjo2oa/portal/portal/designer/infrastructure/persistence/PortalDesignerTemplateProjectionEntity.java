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
@TableName("portal_designer_template_projection")
public class PortalDesignerTemplateProjectionEntity {

    @TableId(value = "template_id", type = IdType.ASSIGN_UUID)
    private String templateId;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("template_code")
    private String templateCode;

    @TableField("scene_type")
    private String sceneType;

    @TableField("versions_json")
    private String versionsJson;

    @TableField("active_publication_ids_json")
    private String activePublicationIdsJson;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
