package com.hjo2oa.biz.collaboration.hub.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_collab_task_participant")
public class TaskParticipantEntity {

    @TableId(value = "participant_id", type = IdType.ASSIGN_UUID)
    private String participantId;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("task_id")
    private String taskId;
    @TableField("person_id")
    private String personId;
    @TableField("role_code")
    private String roleCode;
}
