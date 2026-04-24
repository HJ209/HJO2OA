package com.hjo2oa.data.connector.infrastructure;

public interface HttpConnectivityClient {

    int execute(HttpRequestSpec requestSpec);
}
