package com.hjo2oa.org.org.sync.audit.infrastructure;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.hjo2oa.org.org.sync.audit.infrastructure")
public class OrgSyncAuditMybatisConfiguration {
}
