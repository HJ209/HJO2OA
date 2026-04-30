package com.hjo2oa.biz.collaboration.hub.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("biz_collab_meeting_participant")
public class MeetingParticipantEntity {

    @TableId(value = "participant_id", type = IdType.ASSIGN_UUID)
    private String participantId;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("meeting_id")
    private String meetingId;
    @TableField("person_id")
    private String personId;
    @TableField("role_code")
    private String roleCode;
    @TableField("response_status")
    private String responseStatus;
}
