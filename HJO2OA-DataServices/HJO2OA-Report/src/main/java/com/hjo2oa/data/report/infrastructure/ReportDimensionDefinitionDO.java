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
@TableName("data_report_dimension_def")
public class ReportDimensionDefinitionDO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("report_id")
    private String reportId;

    @TableField("dimension_code")
    private String dimensionCode;

    @TableField("dimension_name")
    private String dimensionName;

    @TableField("dimension_type")
    private String dimensionType;

    @TableField("source_field")
    private String sourceField;

    @TableField("time_granularity")
    private String timeGranularity;

    @TableField("filterable")
    private Boolean filterable;

    @TableField("display_order")
    private Integer displayOrder;

    @TableField("deleted")
    private Boolean deleted;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
