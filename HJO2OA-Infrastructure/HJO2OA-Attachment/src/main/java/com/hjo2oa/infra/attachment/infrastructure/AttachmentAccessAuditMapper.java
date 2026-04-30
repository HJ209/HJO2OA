package com.hjo2oa.infra.attachment.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttachmentAccessAuditMapper extends BaseMapper<AttachmentAccessAuditEntity> {
}
