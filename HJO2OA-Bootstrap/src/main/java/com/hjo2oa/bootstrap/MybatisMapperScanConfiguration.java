package com.hjo2oa.bootstrap;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@MapperScan(annotationClass = Mapper.class, basePackages = {
        "com.hjo2oa.data.common.infrastructure.persistence",
        "com.hjo2oa.data.connector.infrastructure",
        "com.hjo2oa.data.governance.infrastructure.persistence",
        "com.hjo2oa.data.openapi.infrastructure.persistence",
        "com.hjo2oa.data.report.infrastructure",
        "com.hjo2oa.data.service.infrastructure",
        "com.hjo2oa.data.data.sync.infrastructure.persistence",
        "com.hjo2oa.infra.attachment.infrastructure",
        "com.hjo2oa.infra.audit.infrastructure",
        "com.hjo2oa.infra.cache.infrastructure",
        "com.hjo2oa.infra.config.infrastructure",
        "com.hjo2oa.infra.data.i18n.infrastructure",
        "com.hjo2oa.infra.dictionary.infrastructure",
        "com.hjo2oa.infra.errorcode.infrastructure",
        "com.hjo2oa.infra.event.bus.infrastructure",
        "com.hjo2oa.infra.i18n.infrastructure",
        "com.hjo2oa.infra.scheduler.infrastructure.persistence",
        "com.hjo2oa.infra.security.infrastructure",
        "com.hjo2oa.infra.tenant.infrastructure.persistence",
        "com.hjo2oa.infra.timezone.infrastructure",
        "com.hjo2oa.msg.channel.sender.infrastructure",
        "com.hjo2oa.msg.ecosystem.infrastructure",
        "com.hjo2oa.msg.event.subscription.infrastructure",
        "com.hjo2oa.msg.mobile.support.infrastructure",
        "com.hjo2oa.org.data.permission.infrastructure",
        "com.hjo2oa.org.org.structure.infrastructure",
        "com.hjo2oa.org.org.sync.audit.infrastructure",
        "com.hjo2oa.org.person.account.infrastructure",
        "com.hjo2oa.org.position.assignment.infrastructure",
        "com.hjo2oa.org.role.resource.auth.infrastructure",
        "com.hjo2oa.wf.form.metadata.infrastructure",
        "com.hjo2oa.wf.process.definition.infrastructure",
        "com.hjo2oa.wf.process.instance.infrastructure",
        "com.hjo2oa.process.monitor.infrastructure",
        "com.hjo2oa.todo.center.infrastructure"
})
public class MybatisMapperScanConfiguration {
}
