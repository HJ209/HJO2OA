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
@TableName("portal_publication")
public class PortalPublicationEntity {

    @TableId(value = "publication_id", type = IdType.ASSIGN_UUID)
    private String publicationId;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("template_id")
    private String templateId;

    @TableField("scene_type")
    private String sceneType;

    @TableField("client_type")
    private String clientType;

    @TableField("audience_type")
    private String audienceType;

    @TableField("audience_subject_id")
    private String audienceSubjectId;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    @TableField("activated_at")
    private Instant activatedAt;

    @TableField("offlined_at")
    private Instant offlinedAt;
}
