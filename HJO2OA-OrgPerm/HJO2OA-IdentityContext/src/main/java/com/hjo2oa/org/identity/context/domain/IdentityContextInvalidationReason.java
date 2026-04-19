package com.hjo2oa.org.identity.context.domain;

public enum IdentityContextInvalidationReason {
    ASSIGNMENT_REMOVED,
    ASSIGNMENT_EXPIRED,
    PRIMARY_CHANGED,
    POSITION_DISABLED,
    ORGANIZATION_DISABLED,
    PERSON_DISABLED,
    ACCOUNT_LOCKED
}
