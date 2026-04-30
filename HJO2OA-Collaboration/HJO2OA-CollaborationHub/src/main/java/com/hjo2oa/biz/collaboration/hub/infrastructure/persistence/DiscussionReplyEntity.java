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
@TableName("biz_collab_discussion_reply")
public class DiscussionReplyEntity {

    @TableId(value = "reply_id", type = IdType.ASSIGN_UUID)
    private String replyId;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("discussion_id")
    private String discussionId;
    @TableField("author_id")
    private String authorId;
    @TableField("body")
    private String body;
    @TableField("deleted")
    private Boolean deleted;
    @TableField("created_at")
    private Instant createdAt;
    @TableField("deleted_at")
    private Instant deletedAt;
}
