package com.hjo2oa.infra.scheduler.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScheduledJobMapper extends BaseMapper<ScheduledJobEntity> {
}
