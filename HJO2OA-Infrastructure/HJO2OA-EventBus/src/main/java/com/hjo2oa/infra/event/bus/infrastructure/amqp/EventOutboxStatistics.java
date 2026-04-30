package com.hjo2oa.infra.event.bus.infrastructure.amqp;

public record EventOutboxStatistics(
        long pending,
        long published,
        long failed,
        long dead,
        long total
) {
}
