package com.hjo2oa.data.governance.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hjo2oa.data.common.infrastructure.persistence.DataServicesMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
interface HealthCheckRuleMapper extends BaseMapper<HealthCheckRuleEntity>, DataServicesMapper {
}
