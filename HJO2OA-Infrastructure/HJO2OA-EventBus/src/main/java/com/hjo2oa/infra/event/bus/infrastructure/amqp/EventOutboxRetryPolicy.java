package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class EventOutboxRetryPolicy {

    private final EventOutboxRetryProperties properties;

    public EventOutboxRetryPolicy(EventOutboxRetryProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public boolean exhausted(int retryCount) {
        return retryCount >= properties.getMaxRetries();
    }

    public Instant nextRetryAt(Instant now, int retryCount) {
        Objects.requireNonNull(now, "now must not be null");
        long interval = properties.getInitialIntervalSeconds();
        for (int i = 1; i < retryCount; i++) {
            interval = Math.min(
                    properties.getMaxIntervalSeconds(),
                    Math.round(interval * properties.getMultiplier())
            );
        }
        return now.plus(Duration.ofSeconds(Math.min(interval, properties.getMaxIntervalSeconds())));
    }
}
