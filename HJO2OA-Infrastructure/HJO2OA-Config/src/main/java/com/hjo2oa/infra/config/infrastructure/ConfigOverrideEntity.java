package com.hjo2oa.infra.config.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.UUID;

@TableName("infra_config_override")
public class ConfigOverrideEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("config_entry_id")
    private UUID configEntryId;

    @TableField("scope_type")
    private String scopeType;

    @TableField("scope_id")
    private UUID scopeId;

    @TableField("override_value")
    private String overrideValue;

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

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public UUID getScopeId() {
        return scopeId;
    }

    public void setScopeId(UUID scopeId) {
        this.scopeId = scopeId;
    }

    public String getOverrideValue() {
        return overrideValue;
    }

    public void setOverrideValue(String overrideValue) {
        this.overrideValue = overrideValue;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
