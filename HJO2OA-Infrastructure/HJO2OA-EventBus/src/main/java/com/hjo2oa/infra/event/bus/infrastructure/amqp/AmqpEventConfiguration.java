package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConditionalOnProfile
@ConditionalOnProperty(prefix = "hjo2oa.messaging.outbox.amqp", name = "enabled", havingValue = "true")
@EnableScheduling
@Configuration(proxyBeanMethods = false)
public class AmqpEventConfiguration {

    public static final String ROUTING_KEY = "domain.event";

    @Bean
    DirectExchange hjo2oaDomainEventExchange(
            @Value("${hjo2oa.messaging.outbox.amqp.exchange-name:hjo2oa.domain.events}") String exchangeName
    ) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    Queue hjo2oaDomainEventQueue(
            @Value("${hjo2oa.messaging.outbox.amqp.queue-name:hjo2oa.domain.events.local}") String queueName
    ) {
        return new Queue(queueName, true);
    }

    @Bean
    Binding hjo2oaDomainEventBinding(DirectExchange hjo2oaDomainEventExchange, Queue hjo2oaDomainEventQueue) {
        return BindingBuilder.bind(hjo2oaDomainEventQueue)
                .to(hjo2oaDomainEventExchange)
                .with(ROUTING_KEY);
    }
}
