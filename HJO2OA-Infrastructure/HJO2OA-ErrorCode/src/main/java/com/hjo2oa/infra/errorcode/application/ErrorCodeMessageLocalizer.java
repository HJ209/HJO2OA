package com.hjo2oa.infra.errorcode.application;

import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinitionView;

@FunctionalInterface
public interface ErrorCodeMessageLocalizer {

    String localize(ErrorCodeDefinitionView definition, String locale, String fallbackMessage);

    static ErrorCodeMessageLocalizer noop() {
        return (definition, locale, fallbackMessage) -> fallbackMessage;
    }
}
