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
@TableName("biz_collab_workspace")
public class WorkspaceEntity {

    @TableId(value = "workspace_id", type = IdType.ASSIGN_UUID)
    private String workspaceId;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("code")
    private String code;
    @TableField("name")
    private String name;
    @TableField("description")
    private String description;
    @TableField("status")
    private String status;
    @TableField("visibility")
    private String visibility;
    @TableField("owner_id")
    private String ownerId;
    @TableField("created_at")
    private Instant createdAt;
    @TableField("updated_at")
    private Instant updatedAt;
}
