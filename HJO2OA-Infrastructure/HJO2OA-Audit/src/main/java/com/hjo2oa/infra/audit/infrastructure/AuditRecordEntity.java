package com.hjo2oa.infra.audit.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.UUID;

@TableName("infra_audit_record")
public class AuditRecordEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("module_code")
    private String moduleCode;

    @TableField("object_type")
    private String objectType;

    @TableField("object_id")
    private String objectId;

    @TableField("action_type")
    private String actionType;

    @TableField("operator_account_id")
    private UUID operatorAccountId;

    @TableField("operator_person_id")
    private UUID operatorPersonId;

    @TableField("tenant_id")
    private UUID tenantId;

    @TableField("trace_id")
    private String traceId;

    @TableField("summary")
    private String summary;

    @TableField("occurred_at")
    private LocalDateTime occurredAt;

    @TableField("archive_status")
    private String archiveStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public UUID getOperatorAccountId() {
        return operatorAccountId;
    }

    public void setOperatorAccountId(UUID operatorAccountId) {
        this.operatorAccountId = operatorAccountId;
    }

    public UUID getOperatorPersonId() {
        return operatorPersonId;
    }

    public void setOperatorPersonId(UUID operatorPersonId) {
        this.operatorPersonId = operatorPersonId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getArchiveStatus() {
        return archiveStatus;
    }

    public void setArchiveStatus(String archiveStatus) {
        this.archiveStatus = archiveStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
