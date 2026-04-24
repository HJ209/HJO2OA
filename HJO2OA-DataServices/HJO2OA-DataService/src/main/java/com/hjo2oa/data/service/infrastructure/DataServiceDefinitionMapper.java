package com.hjo2oa.data.service.infrastructure;

import com.hjo2oa.data.common.infrastructure.persistence.DataBaseMapper;
import com.hjo2oa.data.common.infrastructure.persistence.DataServicesMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataServiceDefinitionMapper
        extends DataBaseMapper<DataServiceDefinitionEntity>, DataServicesMapper {
}
