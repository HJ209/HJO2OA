package com.hjo2oa.infra.cache.infrastructure;

import com.hjo2oa.shared.messaging.DomainEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheInfrastructureConfiguration {

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    DomainEventPublisher cacheDomainEventPublisher(ApplicationEventPublisher publisher) {
        return publisher::publishEvent;
    }
}
