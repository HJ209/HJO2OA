package com.hjo2oa.data.service.infrastructure;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hjo2oa.data.common.infrastructure.persistence.BaseEntityDO;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("data_service_param_def")
public class ServiceParameterDefinitionEntity extends BaseEntityDO {

    @TableField("service_id")
    private UUID serviceId;

    @TableField("param_code")
    private String paramCode;

    @TableField("param_type")
    private String paramType;

    @TableField("required")
    private Boolean required;

    @TableField("default_value")
    private String defaultValue;

    @TableField("validation_rule_json")
    private String validationRuleJson;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("description")
    private String description;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_by")
    private String createdBy;

    @TableField("updated_by")
    private String updatedBy;
}
