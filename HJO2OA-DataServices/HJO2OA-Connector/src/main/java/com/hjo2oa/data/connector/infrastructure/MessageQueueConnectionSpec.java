package com.hjo2oa.data.connector.infrastructure;

public record MessageQueueConnectionSpec(
        String host,
        int port,
        String virtualHost,
        String username,
        String password
) {
}
