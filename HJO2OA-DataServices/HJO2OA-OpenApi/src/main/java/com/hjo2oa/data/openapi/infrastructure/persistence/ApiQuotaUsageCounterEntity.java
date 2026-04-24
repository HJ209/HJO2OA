package com.hjo2oa.data.openapi.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("data_api_quota_usage_counter")
public class ApiQuotaUsageCounterEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String tenantId;
    private String policyId;
    private String apiId;
    private String clientCode;
    private Instant windowStartedAt;
    private long usedCount;
    private Instant createdAt;
    private Instant updatedAt;
}
