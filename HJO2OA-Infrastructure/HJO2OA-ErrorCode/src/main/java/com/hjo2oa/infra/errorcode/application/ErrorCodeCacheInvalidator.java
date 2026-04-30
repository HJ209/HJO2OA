package com.hjo2oa.infra.errorcode.application;

@FunctionalInterface
public interface ErrorCodeCacheInvalidator {

    void invalidateErrorCodeCaches();
}
