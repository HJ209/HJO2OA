package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventOutboxMapper extends BaseMapper<EventOutboxEntity> {
}
