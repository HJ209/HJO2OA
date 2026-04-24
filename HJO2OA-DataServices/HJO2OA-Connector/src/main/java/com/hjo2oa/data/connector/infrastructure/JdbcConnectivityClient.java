package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.TimeoutRetryConfig;

public interface JdbcConnectivityClient {

    void validate(String jdbcUrl, String username, String password, String validationQuery, TimeoutRetryConfig timeoutRetryConfig);
}
