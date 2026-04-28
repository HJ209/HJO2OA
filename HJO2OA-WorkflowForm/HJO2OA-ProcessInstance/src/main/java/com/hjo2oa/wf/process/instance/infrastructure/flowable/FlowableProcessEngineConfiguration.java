package com.hjo2oa.wf.process.instance.infrastructure.flowable;

import java.util.List;
import javax.sql.DataSource;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.ProcessEngineConfigurationConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "hjo2oa.workflow.engine", havingValue = "flowable", matchIfMissing = true)
public class FlowableProcessEngineConfiguration {

    public static final String FLOWABLE_TABLE_PREFIX = "FLW_";

    @Bean
    public ProcessEngineConfigurationConfigurer hjo2oaProcessEngineConfigurationConfigurer(
            DataSource dataSource,
            FlowableProcessEngineEventBridge eventBridge
    ) {
        return configuration -> configure(configuration, dataSource, eventBridge);
    }

    private void configure(
            SpringProcessEngineConfiguration configuration,
            DataSource dataSource,
            FlowableProcessEngineEventBridge eventBridge
    ) {
        configuration.setDataSource(dataSource);
        configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        configuration.setHistoryLevel(HistoryLevel.FULL);
        configuration.setDatabaseTablePrefix(FLOWABLE_TABLE_PREFIX);
        configuration.setEnableEventDispatcher(true);
        configuration.setEventListeners(List.of(eventBridge));
        configuration.setDeploymentResources(new org.springframework.core.io.Resource[0]);
    }
}
