package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hjo2oa.messaging.outbox.retry")
public class EventOutboxRetryProperties {

    private int maxRetries = 5;
    private long initialIntervalSeconds = 1;
    private double multiplier = 2.0;
    private long maxIntervalSeconds = 60;

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = Math.max(maxRetries, 1);
    }

    public long getInitialIntervalSeconds() {
        return initialIntervalSeconds;
    }

    public void setInitialIntervalSeconds(long initialIntervalSeconds) {
        this.initialIntervalSeconds = Math.max(initialIntervalSeconds, 1);
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = Math.max(multiplier, 1.0);
    }

    public long getMaxIntervalSeconds() {
        return maxIntervalSeconds;
    }

    public void setMaxIntervalSeconds(long maxIntervalSeconds) {
        this.maxIntervalSeconds = Math.max(maxIntervalSeconds, 1);
    }
}
