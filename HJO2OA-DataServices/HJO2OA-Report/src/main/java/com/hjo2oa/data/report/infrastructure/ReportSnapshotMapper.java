package com.hjo2oa.data.report.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hjo2oa.data.common.infrastructure.persistence.DataServicesMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportSnapshotMapper extends BaseMapper<ReportSnapshotDO>, DataServicesMapper {
}
