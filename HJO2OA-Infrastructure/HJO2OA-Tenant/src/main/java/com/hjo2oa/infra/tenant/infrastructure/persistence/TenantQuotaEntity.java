package com.hjo2oa.infra.tenant.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("infra_tenant_quota")
public class TenantQuotaEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("tenant_profile_id")
    private String tenantProfileId;

    @TableField("quota_type")
    private String quotaType;

    @TableField("limit_value")
    private Long limitValue;

    @TableField("used_value")
    private Long usedValue;

    @TableField("warning_threshold")
    private Long warningThreshold;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantProfileId() {
        return tenantProfileId;
    }

    public void setTenantProfileId(String tenantProfileId) {
        this.tenantProfileId = tenantProfileId;
    }

    public String getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(String quotaType) {
        this.quotaType = quotaType;
    }

    public Long getLimitValue() {
        return limitValue;
    }

    public void setLimitValue(Long limitValue) {
        this.limitValue = limitValue;
    }

    public Long getUsedValue() {
        return usedValue;
    }

    public void setUsedValue(Long usedValue) {
        this.usedValue = usedValue;
    }

    public Long getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(Long warningThreshold) {
        this.warningThreshold = warningThreshold;
    }
}
