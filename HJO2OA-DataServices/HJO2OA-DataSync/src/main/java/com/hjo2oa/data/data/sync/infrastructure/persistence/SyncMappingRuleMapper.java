package com.hjo2oa.data.data.sync.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.hjo2oa.data.common.infrastructure.persistence.DataBaseMapper;
import com.hjo2oa.data.common.infrastructure.persistence.DataServicesMapper;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SyncMappingRuleMapper extends DataBaseMapper<SyncMappingRuleDO>, DataServicesMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            <script>
            SELECT id,
                   tenant_id,
                   created_at,
                   updated_at,
                   deleted,
                   sync_task_id,
                   source_field,
                   target_field,
                   transform_rule_json,
                   conflict_strategy,
                   key_mapping,
                   sort_order
              FROM dbo.data_sync_mapping_rule
             WHERE deleted = 0
               AND sync_task_id IN
               <foreach collection="taskIds" item="taskId" open="(" separator="," close=")">
                   #{taskId}
               </foreach>
             ORDER BY sort_order ASC, created_at ASC
            </script>
            """)
    List<SyncMappingRuleDO> selectByTaskIdsForRuntime(@Param("taskIds") List<UUID> taskIds);
}
