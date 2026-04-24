package com.hjo2oa.data.service.infrastructure;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hjo2oa.data.common.infrastructure.persistence.BaseEntityDO;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("data_service_def")
public class DataServiceDefinitionEntity extends BaseEntityDO {

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("service_type")
    private String serviceType;

    @TableField("source_mode")
    private String sourceMode;

    @TableField("permission_mode")
    private String permissionMode;

    @TableField("permission_boundary_json")
    private String permissionBoundaryJson;

    @TableField("cache_policy_json")
    private String cachePolicyJson;

    @TableField("status")
    private String status;

    @TableField("source_ref")
    private String sourceRef;

    @TableField("connector_id")
    private String connectorId;

    @TableField("description")
    private String description;

    @TableField("status_sequence")
    private Integer statusSequence;

    @TableField("activated_at")
    private Instant activatedAt;

    @TableField("created_by")
    private String createdBy;

    @TableField("updated_by")
    private String updatedBy;
}
