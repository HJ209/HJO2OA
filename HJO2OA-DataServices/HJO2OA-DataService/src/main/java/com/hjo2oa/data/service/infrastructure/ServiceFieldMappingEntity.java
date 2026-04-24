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
@TableName("data_service_field_mapping")
public class ServiceFieldMappingEntity extends BaseEntityDO {

    @TableField("service_id")
    private UUID serviceId;

    @TableField("source_field")
    private String sourceField;

    @TableField("target_field")
    private String targetField;

    @TableField("transform_rule_json")
    private String transformRuleJson;

    @TableField("masked")
    private Boolean masked;

    @TableField("description")
    private String description;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_by")
    private String createdBy;

    @TableField("updated_by")
    private String updatedBy;
}
