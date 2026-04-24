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
@TableName("data_governance_action_audit")
class GovernanceActionAuditEntity {

    @TableId(value = "audit_id", type = IdType.INPUT)
    private String auditId;
    private String governanceId;
    private String targetType;
    private String targetCode;
    private String actionType;
    private String actionResult;
    private String operatorId;
    private String operatorName;
    private String reason;
    private String requestId;
    private String payloadJson;
    private String resultMessage;
    private String traceId;
    private Instant createdAt;
    private Instant completedAt;
    @TableLogic
    private Integer deleted;
}
