package com.hjo2oa.infra.audit.infrastructure;

import javax.sql.DataSource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(DataSource.class)
@MapperScan(basePackages = "com.hjo2oa.infra.audit.infrastructure")
public class AuditMybatisConfiguration {
}
