package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.TimeoutRetryConfig;

public interface MessageQueueConnectivityClient {

    void validate(MessageQueueConnectionSpec connectionSpec, TimeoutRetryConfig timeoutRetryConfig);
}
