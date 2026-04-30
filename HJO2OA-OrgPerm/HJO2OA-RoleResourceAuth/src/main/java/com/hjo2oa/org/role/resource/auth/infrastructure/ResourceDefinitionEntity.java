package com.hjo2oa.org.role.resource.auth.infrastructure;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("org_resource")
public class ResourceDefinitionEntity {

    @TableId
    private UUID id;
    private String resourceType;
    private String resourceCode;
    private String name;
    private String parentCode;
    private Integer sortOrder;
    private String status;
    private UUID tenantId;
    private Instant createdAt;
    private Instant updatedAt;
}
