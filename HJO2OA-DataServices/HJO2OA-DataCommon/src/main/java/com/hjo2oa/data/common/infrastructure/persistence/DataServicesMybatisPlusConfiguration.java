package com.hjo2oa.data.common.infrastructure.persistence;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import javax.sql.DataSource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(DataSource.class)
@MapperScan(
        basePackages = "com.hjo2oa.data",
        markerInterface = DataServicesMapper.class
)
public class DataServicesMybatisPlusConfiguration {

    @Bean
    public MybatisPlusInterceptor dataServicesMybatisPlusInterceptor() {
        return new MybatisPlusInterceptor();
    }

    @Bean
    public MetaObjectHandler dataServicesMetaObjectHandler() {
        return new DataServicesMetaObjectHandler();
    }
}
