package com.hjo2oa.data.openapi.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hjo2oa.data.openapi.domain.ApiCredentialStatus;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("data_api_credential_grant")
public class ApiCredentialGrantEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String openApiId;
    private String tenantId;
    private String clientCode;
    private String secretRef;
    private String scopes;
    private Instant expiresAt;
    private ApiCredentialStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
