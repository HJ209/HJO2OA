package com.hjo2oa.msg.channel.sender.infrastructure;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface ChannelDeliveryAttemptMapper extends BaseMapper<DeliveryAttemptEntity> {
}
