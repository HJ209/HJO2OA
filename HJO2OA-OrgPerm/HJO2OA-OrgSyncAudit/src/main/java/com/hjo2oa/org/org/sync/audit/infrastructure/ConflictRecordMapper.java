package com.hjo2oa.org.org.sync.audit.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConflictRecordMapper extends BaseMapper<ConflictRecordEntity> {
}
