package com.hjo2oa.data.governance.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("data_health_check_rule")
class HealthCheckRuleEntity {

    @TableId(value = "rule_id", type = IdType.INPUT)
    private String ruleId;
    private String governanceId;
    private String ruleCode;
    private String ruleName;
    private String checkType;
    private String severity;
    private String status;
    private String metricName;
    private String comparisonOperator;
    private BigDecimal thresholdValue;
    private Integer windowMinutes;
    private Integer dedupMinutes;
    private String scheduleExpression;
    private String strategyJson;
    private Long revision;
    @TableLogic
    private Integer deleted;
    private Instant createdAt;
    private Instant updatedAt;
}
