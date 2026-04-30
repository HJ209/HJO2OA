package com.hjo2oa.infra.tenant.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantProfileMapper extends BaseMapper<TenantProfileEntity> {
}
