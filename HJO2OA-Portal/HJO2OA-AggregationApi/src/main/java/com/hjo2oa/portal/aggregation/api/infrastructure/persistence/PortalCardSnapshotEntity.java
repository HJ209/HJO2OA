package com.hjo2oa.portal.aggregation.api.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("portal_card_snapshot")
public class PortalCardSnapshotEntity {

    @TableId(value = "snapshot_id", type = IdType.ASSIGN_UUID)
    private String snapshotId;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("person_id")
    private String personId;

    @TableField("assignment_id")
    private String assignmentId;

    @TableField("position_id")
    private String positionId;

    @TableField("scene_type")
    private String sceneType;

    @TableField("card_type")
    private String cardType;

    @TableField("state")
    private String state;

    @TableField("data_json")
    private String dataJson;

    @TableField("message")
    private String message;

    @TableField("refreshed_at")
    private Instant refreshedAt;
}
