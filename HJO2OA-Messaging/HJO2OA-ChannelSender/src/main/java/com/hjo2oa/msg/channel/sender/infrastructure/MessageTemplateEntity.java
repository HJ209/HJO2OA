package com.hjo2oa.msg.channel.sender.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("msg_template")
public class MessageTemplateEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("code")
    private String code;

    @TableField("channel_type")
    private String channelType;

    @TableField("locale")
    private String locale;

    @TableField("version")
    private Integer version;

    @TableField("category")
    private String category;

    @TableField("title_template")
    private String titleTemplate;

    @TableField("body_template")
    private String bodyTemplate;

    @TableField("variable_schema")
    private String variableSchema;

    @TableField("status")
    private String status;

    @TableField("system_locked")
    private Boolean systemLocked;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
