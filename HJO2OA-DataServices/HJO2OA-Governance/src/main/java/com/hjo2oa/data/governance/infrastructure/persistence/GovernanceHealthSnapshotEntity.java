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
@TableName("data_governance_health_snapshot")
class GovernanceHealthSnapshotEntity {

    @TableId(value = "snapshot_id", type = IdType.INPUT)
    private String snapshotId;
    private String governanceId;
    private String ruleId;
    private String targetType;
    private String targetCode;
    private String ruleCode;
    private String healthStatus;
    private BigDecimal measuredValue;
    private BigDecimal thresholdValue;
    private String summary;
    private String traceId;
    private Instant checkedAt;
    @TableLogic
    private Integer deleted;
}
