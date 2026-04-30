package com.hjo2oa.data.data.sync.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.hjo2oa.data.common.infrastructure.persistence.DataBaseMapper;
import com.hjo2oa.data.common.infrastructure.persistence.DataServicesMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SyncExchangeTaskMapper extends DataBaseMapper<SyncExchangeTaskDO>, DataServicesMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT id,
                   tenant_id,
                   created_at,
                   updated_at,
                   deleted,
                   code,
                   name,
                   description,
                   task_type,
                   sync_mode,
                   source_connector_id,
                   target_connector_id,
                   dependency_status,
                   checkpoint_mode,
                   checkpoint_config_json,
                   trigger_config_json,
                   retry_policy_json,
                   compensation_policy_json,
                   reconciliation_policy_json,
                   schedule_config_json,
                   status
              FROM dbo.data_sync_task
             WHERE status = #{status}
               AND deleted = 0
             ORDER BY updated_at DESC
            """)
    List<SyncExchangeTaskDO> selectActiveForRuntime(@Param("status") String status);
}
