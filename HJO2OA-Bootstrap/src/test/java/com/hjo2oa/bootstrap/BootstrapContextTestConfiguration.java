package com.hjo2oa.bootstrap;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
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

    private static final class MapperDefinitionPruner
            implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            // MapperFactoryBean definitions are not yet registered at this phase
            // (MapperScannerConfigurer runs later), so nothing to do here.
        }

        private boolean isMapperFactoryBeanDefinition(BeanDefinition beanDefinition) {
            String beanClassName = beanDefinition.getBeanClassName();
            if ("org.mybatis.spring.mapper.MapperFactoryBean".equals(beanClassName)) {
                return true;
            }
            if (beanClassName != null && beanClassName.startsWith("com.hjo2oa.")) {
                try {
                    Class<?> beanClass = Class.forName(beanClassName);
                    return beanClass.isInterface() && beanClass.isAnnotationPresent(Mapper.class);
                } catch (ClassNotFoundException ex) {
                    return false;
                }
            }
            if (beanDefinition instanceof org.springframework.beans.factory.support.AbstractBeanDefinition abd) {
                try {
                    Class<?> beanClass = abd.getBeanClass();
                    if (beanClass != null
                            && org.mybatis.spring.mapper.MapperFactoryBean.class.isAssignableFrom(beanClass)) {
                        return true;
                    }
                } catch (IllegalStateException ex) {
                    // getBeanClass() throws when no class specified
                }
            }
            return false;
        }

        @Override
        public void postProcessBeanFactory(
                org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory
        ) throws BeansException {
            if (beanFactory instanceof BeanDefinitionRegistry registry) {
                // Collect mapper bean names and their mapper interface types before removing
                List<String> mapperBeanNames = new ArrayList<>();
                List<String> mapperInterfaceNames = new ArrayList<>();
                for (String name : beanFactory.getBeanDefinitionNames()) {
                    BeanDefinition bd = beanFactory.getBeanDefinition(name);
                    if (isMapperFactoryBeanDefinition(bd)) {
                        mapperBeanNames.add(name);
                        // For MapperFactoryBean, the mapperInterface is stored as a constructor arg or property
                        String mapperInterface = bd.getBeanClassName();
                        if ("org.mybatis.spring.mapper.MapperFactoryBean".equals(mapperInterface)) {
                            // Try to get the mapperInterface from constructor arguments
                            if (bd.getConstructorArgumentValues().getGenericArgumentValues().size() == 1) {
                                Object argValue = bd.getConstructorArgumentValues()
                                        .getGenericArgumentValues().iterator().next().getValue();
                                if (argValue instanceof String) {
                                    mapperInterface = (String) argValue;
                                }
                            }
                        }
                        mapperInterfaceNames.add(mapperInterface);
                    }
                }

                // Remove MapperFactoryBean definitions — they cannot be instantiated without
                // real MyBatis infrastructure (SqlSessionFactory, etc.).
                for (String name : mapperBeanNames) {
                    registry.removeBeanDefinition(name);
                }

                // Register Mockito mock beans for each removed mapper interface
                for (int i = 0; i < mapperBeanNames.size(); i++) {
                    String beanName = mapperBeanNames.get(i);
                    String interfaceName = mapperInterfaceNames.get(i);
                    try {
                        Class<?> mapperInterface = Class.forName(interfaceName);
                        Object mock = org.mockito.Mockito.mock(mapperInterface);
                        org.springframework.beans.factory.support.RootBeanDefinition mockBd =
                                new org.springframework.beans.factory.support.RootBeanDefinition();
                        mockBd.setTargetType(mapperInterface);
                        mockBd.setInstanceSupplier(() -> mock);
                        mockBd.setScope(BeanDefinition.SCOPE_SINGLETON);
                        registry.registerBeanDefinition(beanName, mockBd);
                    } catch (ClassNotFoundException ex) {
                        // Skip if class not found
                    }
                }

            }
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}
