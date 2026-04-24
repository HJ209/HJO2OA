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
@TableName("data_report_metric_def")
public class ReportMetricDefinitionDO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("report_id")
    private String reportId;

    @TableField("metric_code")
    private String metricCode;

    @TableField("metric_name")
    private String metricName;

    @TableField("aggregation_type")
    private String aggregationType;

    @TableField("source_field")
    private String sourceField;

    @TableField("formula")
    private String formula;

    @TableField("filter_expression")
    private String filterExpression;

    @TableField("unit")
    private String unit;

    @TableField("trend_enabled")
    private Boolean trendEnabled;

    @TableField("rank_enabled")
    private Boolean rankEnabled;

    @TableField("display_order")
    private Integer displayOrder;

    @TableField("deleted")
    private Boolean deleted;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
