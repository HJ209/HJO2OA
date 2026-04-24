package com.hjo2oa.data.data.sync.infrastructure.persistence;

import com.hjo2oa.data.common.infrastructure.persistence.DataBaseMapper;
import com.hjo2oa.data.common.infrastructure.persistence.DataServicesMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SyncExchangeTaskMapper extends DataBaseMapper<SyncExchangeTaskDO>, DataServicesMapper {
}
