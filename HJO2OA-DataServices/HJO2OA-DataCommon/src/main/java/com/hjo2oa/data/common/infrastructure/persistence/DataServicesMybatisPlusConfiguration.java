package com.hjo2oa.data.common.infrastructure.persistence;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.hjo2oa.shared.tenant.SharedTenantLineHandler;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DataServicesMybatisPlusConfiguration {

    @Bean
    public MybatisPlusInterceptor dataServicesMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new SharedTenantLineHandler()));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler dataServicesMetaObjectHandler() {
        return new DataServicesMetaObjectHandler();
    }
}
