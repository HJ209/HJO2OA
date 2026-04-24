package com.hjo2oa.infra.tenant.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hjo2oa.data.common.infrastructure.persistence.DataServicesMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantProfileMapper extends BaseMapper<TenantProfileEntity>, DataServicesMapper {
}
