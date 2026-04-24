package com.hjo2oa.data.connector.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hjo2oa.data.common.infrastructure.persistence.DataServicesMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConnectorParameterMapper extends BaseMapper<ConnectorParameterDO>, DataServicesMapper {
}
