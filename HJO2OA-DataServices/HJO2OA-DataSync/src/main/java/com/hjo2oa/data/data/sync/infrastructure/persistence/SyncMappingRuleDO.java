package com.hjo2oa.data.data.sync.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hjo2oa.data.common.infrastructure.persistence.BaseEntityDO;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("data_sync_mapping_rule")
public class SyncMappingRuleDO extends BaseEntityDO {

    @TableField("sync_task_id")
    private UUID syncTaskId;

    @TableField("source_field")
    private String sourceField;

    @TableField("target_field")
    private String targetField;

    @TableField("transform_rule_json")
    private String transformRuleJson;

    @TableField("conflict_strategy")
    private String conflictStrategy;

    @TableField("key_mapping")
    private Boolean keyMapping;

    @TableField("sort_order")
    private Integer sortOrder;
}
