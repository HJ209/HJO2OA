package com.hjo2oa.msg.mobile.support.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("msg_mobile_push_preference")
public class MobilePushPreferenceEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("person_id")
    private UUID personId;

    @TableField("push_enabled")
    private Boolean pushEnabled;

    @TableField("quiet_starts_at")
    private LocalTime quietStartsAt;

    @TableField("quiet_ends_at")
    private LocalTime quietEndsAt;

    @TableField("muted_categories")
    private String mutedCategories;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
