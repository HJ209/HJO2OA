package com.hjo2oa.data.openapi.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hjo2oa.data.openapi.domain.ApiInvocationOutcome;
import com.hjo2oa.data.openapi.domain.OpenApiAuthType;
import com.hjo2oa.data.openapi.domain.OpenApiHttpMethod;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("data_api_invocation_audit_log")
public class ApiInvocationAuditLogEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String requestId;
    private String tenantId;
    private String apiId;
    private String endpointCode;
    private String endpointVersion;
    private String path;
    private OpenApiHttpMethod httpMethod;
    private String clientCode;
    private OpenApiAuthType authType;
    private ApiInvocationOutcome outcome;
    private int responseStatus;
    private String errorCode;
    private long durationMs;
    private String requestDigest;
    private String remoteIp;
    private Instant occurredAt;
    private boolean abnormalFlag;
    private String reviewConclusion;
    private String note;
    private String reviewedBy;
    private Instant reviewedAt;
}
