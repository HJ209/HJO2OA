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
@TableName("biz_collab_meeting_reminder")
public class MeetingReminderEntity {

    @TableId(value = "reminder_id", type = IdType.ASSIGN_UUID)
    private String reminderId;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("meeting_id")
    private String meetingId;
    @TableField("participant_id")
    private String participantId;
    @TableField("remind_at")
    private Instant remindAt;
    @TableField("status")
    private String status;
    @TableField("sent_at")
    private Instant sentAt;
}
