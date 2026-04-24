package com.hjo2oa.data.connector.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("data_connector_health_snapshot")
public class ConnectorHealthSnapshotDO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("connector_id")
    private String connectorId;

    @TableField("check_type")
    private String checkType;

    @TableField("health_status")
    private String healthStatus;

    @TableField("latency_ms")
    private Long latencyMs;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_summary")
    private String errorSummary;

    @TableField("operator_id")
    private String operatorId;

    @TableField("target_environment")
    private String targetEnvironment;

    @TableField("confirmed_by")
    private String confirmedBy;

    @TableField("confirmation_note")
    private String confirmationNote;

    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    @TableField("change_sequence")
    private Long changeSequence;

    @TableField("checked_at")
    private LocalDateTime checkedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getTargetEnvironment() {
        return targetEnvironment;
    }

    public void setTargetEnvironment(String targetEnvironment) {
        this.targetEnvironment = targetEnvironment;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public String getConfirmationNote() {
        return confirmationNote;
    }

    public void setConfirmationNote(String confirmationNote) {
        this.confirmationNote = confirmationNote;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Long getChangeSequence() {
        return changeSequence;
    }

    public void setChangeSequence(Long changeSequence) {
        this.changeSequence = changeSequence;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
}
