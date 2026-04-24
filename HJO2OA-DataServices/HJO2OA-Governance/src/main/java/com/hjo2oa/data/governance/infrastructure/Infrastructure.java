package com.hjo2oa.data.governance.infrastructure;

import com.hjo2oa.shared.messaging.DomainEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class Infrastructure {

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    DomainEventPublisher governanceDomainEventPublisher(ApplicationEventPublisher publisher) {
        return publisher::publishEvent;
    }
}
