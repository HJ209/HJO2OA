package com.hjo2oa.infra.timezone.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TimezoneSettingMapper extends BaseMapper<TimezoneSettingEntity> {
}
