package com.hjo2oa.org.org.sync.audit.infrastructure;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface OrgSyncAuditRecordMapper extends BaseMapper<AuditRecordEntity> {
}
