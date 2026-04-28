package com.hjo2oa.bootstrap;

import java.util.Arrays;
import javax.sql.DataSource;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

@TestConfiguration(proxyBeanMethods = false)
public class BootstrapContextTestConfiguration {

    @Bean
    static BeanDefinitionRegistryPostProcessor mybatisMapperDefinitionPruner() {
        return new MapperDefinitionPruner();
    }

    @Bean
    DataSource dataSource() {
        return org.mockito.Mockito.mock(DataSource.class);
    }

    @Bean
    org.mybatis.spring.SqlSessionTemplate sqlSessionTemplate() {
        org.mybatis.spring.SqlSessionTemplate sqlSessionTemplate =
                org.mockito.Mockito.mock(org.mybatis.spring.SqlSessionTemplate.class);
        org.mockito.Mockito.when(sqlSessionTemplate.getConfiguration())
                .thenReturn(new org.apache.ibatis.session.Configuration());
        return sqlSessionTemplate;
    }

    @Bean
    BeanPostProcessor mapperFactoryBeanSqlSessionTemplateInjector(
            org.mybatis.spring.SqlSessionTemplate sqlSessionTemplate
    ) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof org.mybatis.spring.mapper.MapperFactoryBean<?> mapperFactoryBean) {
                    mapperFactoryBean.setSqlSessionTemplate(sqlSessionTemplate);
                }
                return bean;
            }
        };
    }

    private static final class MapperDefinitionPruner
            implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            Arrays.stream(registry.getBeanDefinitionNames())
                    .map(registry::getBeanDefinition)
                    .filter(this::shouldPrepareMapperDefinition)
                    .forEach(beanDefinition -> beanDefinition.getPropertyValues()
                            .add("sqlSessionTemplate", new RuntimeBeanReference("sqlSessionTemplate")));
        }

        private boolean shouldPrepareMapperDefinition(BeanDefinition beanDefinition) {
            String beanClassName = beanDefinition.getBeanClassName();
            if ("org.mybatis.spring.mapper.MapperFactoryBean".equals(beanClassName)) {
                return true;
            }
            if (beanClassName == null || !beanClassName.startsWith("com.hjo2oa.")) {
                return false;
            }
            try {
                Class<?> beanClass = Class.forName(beanClassName);
                return beanClass.isInterface() && beanClass.isAnnotationPresent(Mapper.class);
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }

        @Override
        public void postProcessBeanFactory(
                org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory
        ) throws BeansException {
            // No-op.
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}
