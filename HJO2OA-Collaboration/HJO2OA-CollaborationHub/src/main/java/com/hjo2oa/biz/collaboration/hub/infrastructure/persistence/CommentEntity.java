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
@TableName("biz_collab_comment")
public class CommentEntity {

    @TableId(value = "comment_id", type = IdType.ASSIGN_UUID)
    private String commentId;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("workspace_id")
    private String workspaceId;
    @TableField("object_type")
    private String objectType;
    @TableField("object_id")
    private String objectId;
    @TableField("author_id")
    private String authorId;
    @TableField("body")
    private String body;
    @TableField("deleted")
    private Boolean deleted;
    @TableField("created_at")
    private Instant createdAt;
    @TableField("updated_at")
    private Instant updatedAt;
    @TableField("deleted_at")
    private Instant deletedAt;
}
