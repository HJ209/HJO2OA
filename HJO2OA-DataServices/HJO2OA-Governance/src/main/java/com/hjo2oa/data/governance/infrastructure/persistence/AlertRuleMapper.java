package com.hjo2oa.data.governance.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hjo2oa.data.common.infrastructure.persistence.DataServicesMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface AlertRuleMapper extends BaseMapper<AlertRuleEntity>, DataServicesMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM dbo.data_alert_rule WHERE governance_id = #{governanceId}")
    int deleteByGovernanceIdPhysically(@Param("governanceId") String governanceId);
}
