package com.hjo2oa.infra.config.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.UUID;

@TableName("infra_config_entry")
public class ConfigEntryEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("config_key")
    private String configKey;

    @TableField("name")
    private String name;

    @TableField("config_type")
    private String configType;

    @TableField("default_value")
    private String defaultValue;

    @TableField("validation_rule")
    private String validationRule;

    @TableField("mutable_at_runtime")
    private Boolean mutableAtRuntime;

    @TableField("status")
    private String status;

    @TableField("tenant_aware")
    private Boolean tenantAware;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getValidationRule() {
        return validationRule;
    }

    public void setValidationRule(String validationRule) {
        this.validationRule = validationRule;
    }

    public Boolean getMutableAtRuntime() {
        return mutableAtRuntime;
    }

    public void setMutableAtRuntime(Boolean mutableAtRuntime) {
        this.mutableAtRuntime = mutableAtRuntime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getTenantAware() {
        return tenantAware;
    }

    public void setTenantAware(Boolean tenantAware) {
        this.tenantAware = tenantAware;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
