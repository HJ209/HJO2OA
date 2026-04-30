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
@TableName("data_governance_runtime_signal")
class GovernanceRuntimeSignalEntity {

    @TableId(value = "signal_id", type = IdType.INPUT)
    private String signalId;
    private String tenantId;
    private String targetType;
    private String targetCode;
    private String runtimeStatus;
    private Long totalExecutions;
    private Long failureCount;
    private BigDecimal failureRate;
    private Long lastDurationMs;
    private Long freshnessLagSeconds;
    private Instant lastSuccessAt;
    private Instant lastFailureAt;
    private String lastErrorCode;
    private String lastErrorMessage;
    private String lastEventType;
    private String lastExecutionId;
    private String traceId;
    private String payloadJson;
    private Long revision;
    @TableLogic
    private Integer deleted;
    private Instant createdAt;
    private Instant updatedAt;
}
