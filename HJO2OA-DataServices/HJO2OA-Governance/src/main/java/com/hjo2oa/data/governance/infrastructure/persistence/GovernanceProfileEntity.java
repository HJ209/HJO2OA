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
@TableName("data_governance_profile")
class GovernanceProfileEntity {

    @TableId(value = "governance_id", type = IdType.INPUT)
    private String governanceId;
    private String code;
    private String scopeType;
    private String targetCode;
    private String slaPolicyJson;
    private String alertPolicyJson;
    private String status;
    private String tenantId;
    private Long revision;
    @TableLogic
    private Integer deleted;
    private Instant createdAt;
    private Instant updatedAt;
}
