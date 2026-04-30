package com.hjo2oa.biz.collaboration.hub.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_collab_attachment_link")
public class AttachmentLinkEntity {

    @TableId(value = "attachment_link_id", type = IdType.ASSIGN_UUID)
    private String attachmentLinkId;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("owner_type")
    private String ownerType;
    @TableField("owner_id")
    private String ownerId;
    @TableField("attachment_id")
    private String attachmentId;
    @TableField("file_name")
    private String fileName;
    @TableField("created_at")
    private Instant createdAt;
}
