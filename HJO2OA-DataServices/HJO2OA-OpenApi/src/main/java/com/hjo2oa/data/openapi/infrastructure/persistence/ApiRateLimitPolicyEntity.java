package com.hjo2oa.data.openapi.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hjo2oa.data.openapi.domain.ApiPolicyStatus;
import com.hjo2oa.data.openapi.domain.ApiPolicyType;
import com.hjo2oa.data.openapi.domain.ApiWindowUnit;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("data_api_rate_limit_policy")
public class ApiRateLimitPolicyEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String openApiId;
    private String tenantId;
    private String policyCode;
    private String clientCode;
    private ApiPolicyType policyType;
    private long windowValue;
    private ApiWindowUnit windowUnit;
    private long threshold;
    private ApiPolicyStatus status;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
