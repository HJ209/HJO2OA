package com.hjo2oa.data.report.infrastructure;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class ReportRuntimeConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock reportClock() {
        return Clock.systemUTC();
    }
}
