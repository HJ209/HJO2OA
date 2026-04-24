package com.hjo2oa.infra.audit.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditFieldChangeMapper extends BaseMapper<AuditFieldChangeEntity> {
}
