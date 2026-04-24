package com.hjo2oa.infra.config.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.UUID;

@TableName("infra_feature_rule")
public class FeatureRuleEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("config_entry_id")
    private UUID configEntryId;

    @TableField("rule_type")
    private String ruleType;

    @TableField("rule_value")
    private String ruleValue;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("active")
    private Boolean active;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getConfigEntryId() {
        return configEntryId;
    }

    public void setConfigEntryId(UUID configEntryId) {
        this.configEntryId = configEntryId;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getRuleValue() {
        return ruleValue;
    }

    public void setRuleValue(String ruleValue) {
        this.ruleValue = ruleValue;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
