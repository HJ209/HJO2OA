package com.hjo2oa.data.governance.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("data_service_version_record")
class ServiceVersionRecordEntity {

    @TableId(value = "version_record_id", type = IdType.INPUT)
    private String versionRecordId;
    private String governanceId;
    private String targetType;
    private String targetCode;
    @TableField("version_no")
    private String version;
    private String compatibilityNote;
    private String changeSummary;
    private String status;
    private Instant registeredAt;
    private Instant publishedAt;
    private Instant deprecatedAt;
    private String operatorId;
    private String approvalNote;
    private String auditTraceId;
    @Version
    private Long revision;
    @TableLogic
    private Integer deleted;
    private Instant createdAt;
    private Instant updatedAt;
}
