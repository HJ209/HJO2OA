package com.hjo2oa.data.report.infrastructure;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hjo2oa.data.common.infrastructure.persistence.DataServicesMapper;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportDefinitionMapper extends BaseMapper<ReportDefinitionDO>, DataServicesMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT id,
                   code,
                   name,
                   report_type,
                   source_scope,
                   refresh_mode,
                   visibility_mode,
                   status,
                   tenant_id,
                   definition_version,
                   caliber_definition,
                   refresh_config,
                   card_protocol,
                   last_refreshed_at,
                   last_freshness_status,
                   last_refresh_batch,
                   next_refresh_at,
                   deleted,
                   created_at,
                   updated_at
              FROM dbo.data_report_def
             WHERE refresh_mode = #{refreshMode}
               AND status = #{status}
               AND deleted = 0
               AND next_refresh_at <= #{now}
             ORDER BY next_refresh_at ASC
            """)
    List<ReportDefinitionDO> selectDueScheduledForRuntime(
            @Param("refreshMode") String refreshMode,
            @Param("status") String status,
            @Param("now") Instant now
    );

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT tenant_id
              FROM dbo.data_report_def
             WHERE id = #{reportId}
               AND deleted = 0
            """)
    String selectTenantIdByReportId(@Param("reportId") String reportId);
}
