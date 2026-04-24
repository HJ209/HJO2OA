package com.hjo2oa.data.governance.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("data_governance_alert_record")
class GovernanceAlertRecordEntity {

    @TableId(value = "alert_id", type = IdType.INPUT)
    private String alertId;
    private String governanceId;
    private String ruleId;
    private String targetType;
    private String targetCode;
    private String alertLevel;
    private String alertType;
    private String status;
    private String alertKey;
    private String summary;
    private String detail;
    private String traceId;
    private Instant occurredAt;
    private Instant acknowledgedAt;
    private String acknowledgedBy;
    private Instant escalatedAt;
    private String escalatedBy;
    private Instant closedAt;
    private String closedBy;
    private String closeReason;
    @TableLogic
    private Integer deleted;
}
