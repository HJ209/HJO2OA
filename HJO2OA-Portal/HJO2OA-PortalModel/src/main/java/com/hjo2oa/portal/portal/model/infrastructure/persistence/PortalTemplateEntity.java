package com.hjo2oa.portal.portal.model.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("portal_template")
public class PortalTemplateEntity {

    @TableId(value = "template_id", type = IdType.ASSIGN_UUID)
    private String templateId;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("template_code")
    private String templateCode;

    @TableField("display_name")
    private String displayName;

    @TableField("scene_type")
    private String sceneType;

    @TableField("pages_json")
    private String pagesJson;

    @TableField("versions_json")
    private String versionsJson;

    @TableField("published_snapshots_json")
    private String publishedSnapshotsJson;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
