package com.hjo2oa.data.governance.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("data_alert_rule")
class AlertRuleEntity {

    @TableId(value = "rule_id", type = IdType.INPUT)
    private String ruleId;
    private String governanceId;
    private String ruleCode;
    private String ruleName;
    private String sourceRuleCode;
    private String metricName;
    private String alertType;
    private String alertLevel;
    private String status;
    private String comparisonOperator;
    private BigDecimal thresholdValue;
    private Integer dedupMinutes;
    private Integer escalationMinutes;
    private String notificationPolicyJson;
    private String strategyJson;
    @Version
    private Long revision;
    @TableLogic
    private Integer deleted;
    private Instant createdAt;
    private Instant updatedAt;
}
