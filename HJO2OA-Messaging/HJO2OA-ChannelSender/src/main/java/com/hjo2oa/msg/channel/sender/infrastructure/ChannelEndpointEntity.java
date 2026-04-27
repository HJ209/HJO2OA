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
@TableName("msg_channel_endpoint")
public class ChannelEndpointEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("endpoint_code")
    private String endpointCode;

    @TableField("channel_type")
    private String channelType;

    @TableField("provider_type")
    private String providerType;

    @TableField("display_name")
    private String displayName;

    @TableField("status")
    private String status;

    @TableField("config_ref")
    private String configRef;

    @TableField("rate_limit_per_minute")
    private Integer rateLimitPerMinute;

    @TableField("daily_quota")
    private Integer dailyQuota;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
