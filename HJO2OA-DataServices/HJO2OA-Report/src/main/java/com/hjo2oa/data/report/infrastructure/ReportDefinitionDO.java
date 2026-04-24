package com.hjo2oa.data.report.infrastructure;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("data_report_def")
public class ReportDefinitionDO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("report_type")
    private String reportType;

    @TableField("source_scope")
    private String sourceScope;

    @TableField("refresh_mode")
    private String refreshMode;

    @TableField("visibility_mode")
    private String visibilityMode;

    @TableField("status")
    private String status;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("definition_version")
    private Integer definitionVersion;

    @TableField("caliber_definition")
    private String caliberDefinition;

    @TableField("refresh_config")
    private String refreshConfig;

    @TableField("card_protocol")
    private String cardProtocol;

    @TableField("last_refreshed_at")
    private Instant lastRefreshedAt;

    @TableField("last_freshness_status")
    private String lastFreshnessStatus;

    @TableField("last_refresh_batch")
    private String lastRefreshBatch;

    @TableField("next_refresh_at")
    private Instant nextRefreshAt;

    @TableField("deleted")
    private Boolean deleted;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
