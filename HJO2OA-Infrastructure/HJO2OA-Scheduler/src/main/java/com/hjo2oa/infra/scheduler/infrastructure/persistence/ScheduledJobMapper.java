package com.hjo2oa.infra.scheduler.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ScheduledJobMapper extends BaseMapper<ScheduledJobEntity> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT id,
                   job_code,
                   handler_name,
                   name,
                   trigger_type,
                   cron_expr,
                   timezone_id,
                   concurrency_policy,
                   timeout_seconds,
                   retry_policy,
                   status,
                   tenant_id,
                   created_at,
                   updated_at
              FROM dbo.infra_scheduled_job
            """)
    List<ScheduledJobEntity> selectAllForRuntime();
}
