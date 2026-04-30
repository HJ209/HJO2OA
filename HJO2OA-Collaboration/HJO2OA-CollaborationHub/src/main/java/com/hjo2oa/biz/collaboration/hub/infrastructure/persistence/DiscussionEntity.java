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
@TableName("biz_collab_discussion")
public class DiscussionEntity {

    @TableId(value = "discussion_id", type = IdType.ASSIGN_UUID)
    private String discussionId;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("workspace_id")
    private String workspaceId;
    @TableField("title")
    private String title;
    @TableField("body")
    private String body;
    @TableField("author_id")
    private String authorId;
    @TableField("status")
    private String status;
    @TableField("pinned")
    private Boolean pinned;
    @TableField("created_at")
    private Instant createdAt;
    @TableField("updated_at")
    private Instant updatedAt;
    @TableField("closed_at")
    private Instant closedAt;
}
