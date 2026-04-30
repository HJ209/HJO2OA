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
@TableName("biz_collab_discussion_read")
public class DiscussionReadEntity {

    @TableId(value = "read_id", type = IdType.ASSIGN_UUID)
    private String readId;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("discussion_id")
    private String discussionId;
    @TableField("person_id")
    private String personId;
    @TableField("read_at")
    private Instant readAt;
}
