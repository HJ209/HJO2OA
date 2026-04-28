package com.hjo2oa.msg.message.center.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationProjectionEventMapper extends BaseMapper<NotificationProjectionEventEntity> {
}
