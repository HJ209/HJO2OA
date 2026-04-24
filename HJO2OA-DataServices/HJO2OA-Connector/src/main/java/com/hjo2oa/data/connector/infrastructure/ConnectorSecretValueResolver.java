package com.hjo2oa.data.connector.infrastructure;

public interface ConnectorSecretValueResolver {

    String resolve(String paramValueRef, boolean sensitive);
}
