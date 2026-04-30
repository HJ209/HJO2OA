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
@TableName("biz_collab_meeting")
public class MeetingEntity {

    @TableId(value = "meeting_id", type = IdType.ASSIGN_UUID)
    private String meetingId;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("workspace_id")
    private String workspaceId;
    @TableField("title")
    private String title;
    @TableField("agenda")
    private String agenda;
    @TableField("organizer_id")
    private String organizerId;
    @TableField("status")
    private String status;
    @TableField("start_at")
    private Instant startAt;
    @TableField("end_at")
    private Instant endAt;
    @TableField("location")
    private String location;
    @TableField("reminder_minutes_before")
    private Integer reminderMinutesBefore;
    @TableField("minutes")
    private String minutes;
    @TableField("minutes_published_at")
    private Instant minutesPublishedAt;
    @TableField("created_at")
    private Instant createdAt;
    @TableField("updated_at")
    private Instant updatedAt;
}
