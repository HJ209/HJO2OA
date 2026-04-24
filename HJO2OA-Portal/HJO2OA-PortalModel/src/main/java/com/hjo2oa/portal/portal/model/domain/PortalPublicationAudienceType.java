package com.hjo2oa.portal.portal.model.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PortalPublicationAudienceType {
    TENANT_DEFAULT("tenant-default"),
    ASSIGNMENT("assignment"),
    PERSON("person"),
    POSITION("position");

    private final String value;

    PortalPublicationAudienceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
