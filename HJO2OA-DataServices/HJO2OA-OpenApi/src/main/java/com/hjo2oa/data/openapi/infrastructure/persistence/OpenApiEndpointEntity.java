package com.hjo2oa.data.openapi.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hjo2oa.data.openapi.domain.OpenApiAuthType;
import com.hjo2oa.data.openapi.domain.OpenApiHttpMethod;
import com.hjo2oa.data.openapi.domain.OpenApiStatus;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("data_open_api_endpoint")
public class OpenApiEndpointEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String tenantId;
    private String code;
    private String name;
    private String dataServiceId;
    private String dataServiceCode;
    private String dataServiceName;
    private String path;
    private OpenApiHttpMethod httpMethod;
    private String version;
    private OpenApiAuthType authType;
    private String compatibilityNotes;
    private OpenApiStatus status;
    private Instant publishedAt;
    private Instant deprecatedAt;
    private Instant sunsetAt;
    private Instant createdAt;
    private Instant updatedAt;
}
