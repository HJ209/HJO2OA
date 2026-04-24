package com.hjo2oa.infra.audit.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.UUID;

@TableName("infra_audit_field_change")
public class AuditFieldChangeEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("audit_record_id")
    private UUID auditRecordId;

    @TableField("field_name")
    private String fieldName;

    @TableField("old_value")
    private String oldValue;

    @TableField("new_value")
    private String newValue;

    @TableField("sensitivity_level")
    private String sensitivityLevel;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAuditRecordId() {
        return auditRecordId;
    }

    public void setAuditRecordId(UUID auditRecordId) {
        this.auditRecordId = auditRecordId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getSensitivityLevel() {
        return sensitivityLevel;
    }

    public void setSensitivityLevel(String sensitivityLevel) {
        this.sensitivityLevel = sensitivityLevel;
    }
}
