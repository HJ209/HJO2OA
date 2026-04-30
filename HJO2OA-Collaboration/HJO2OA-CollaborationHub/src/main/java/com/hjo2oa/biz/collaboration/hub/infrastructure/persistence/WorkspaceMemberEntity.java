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
@TableName("biz_collab_workspace_member")
public class WorkspaceMemberEntity {

    @TableId(value = "member_id", type = IdType.ASSIGN_UUID)
    private String memberId;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("workspace_id")
    private String workspaceId;
    @TableField("person_id")
    private String personId;
    @TableField("role_code")
    private String roleCode;
    @TableField("permissions")
    private String permissions;
    @TableField("status")
    private String status;
    @TableField("joined_at")
    private Instant joinedAt;
    @TableField("updated_at")
    private Instant updatedAt;
}
