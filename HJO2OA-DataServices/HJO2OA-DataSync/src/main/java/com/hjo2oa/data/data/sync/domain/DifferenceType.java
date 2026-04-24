package com.hjo2oa.data.data.sync.domain;

public enum DifferenceType {
    MISSING_TARGET,
    EXTRA_TARGET,
    VALUE_MISMATCH,
    CONFLICT,
    WRITE_FAILURE
}
