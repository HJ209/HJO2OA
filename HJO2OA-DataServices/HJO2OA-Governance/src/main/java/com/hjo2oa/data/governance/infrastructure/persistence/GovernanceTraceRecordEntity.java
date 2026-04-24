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
@TableName("data_governance_trace_record")
class GovernanceTraceRecordEntity {

    @TableId(value = "trace_id", type = IdType.INPUT)
    private String traceId;
    private String governanceId;
    private String targetType;
    private String targetCode;
    private String traceType;
    private String status;
    private String sourceEventType;
    private String sourceExecutionId;
    private String correlationId;
    private String summary;
    private String detail;
    private Instant openedAt;
    private Instant updatedAt;
    private Instant resolvedAt;
    @TableLogic
    private Integer deleted;
}
