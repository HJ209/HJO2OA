package com.hjo2oa.infra.i18n.application;

@FunctionalInterface
public interface I18nCacheInvalidationListener {

    void onI18nCacheInvalidated();
}
