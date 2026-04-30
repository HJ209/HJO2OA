package com.hjo2oa.data.governance.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hjo2oa.data.common.infrastructure.persistence.DataServicesMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface GovernanceProfileMapper extends BaseMapper<GovernanceProfileEntity>, DataServicesMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT governance_id,
                   code,
                   scope_type,
                   target_code,
                   sla_policy_json,
                   alert_policy_json,
                   status,
                   tenant_id,
                   revision,
                   deleted,
                   created_at,
                   updated_at
              FROM dbo.data_governance_profile
             WHERE status = #{status}
               AND deleted = 0
            """)
    List<GovernanceProfileEntity> selectAllActiveForRuntime(@Param("status") String status);
}
