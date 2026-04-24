package com.hjo2oa.infra.i18n.interfaces;

import com.hjo2oa.infra.i18n.domain.LocaleBundleView;
import com.hjo2oa.infra.i18n.domain.LocaleResourceEntryView;
import com.hjo2oa.infra.i18n.domain.ResolvedLocaleMessageView;
import org.springframework.stereotype.Component;

@Component
public class LocaleBundleDtoMapper {

    public LocaleBundleDtos.BundleResponse toBundleResponse(LocaleBundleView view) {
        return new LocaleBundleDtos.BundleResponse(
                view.id(),
                view.bundleCode(),
                view.moduleCode(),
                view.locale(),
                view.fallbackLocale(),
                view.status(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt(),
                view.entries().stream().map(this::toEntryResponse).toList()
        );
    }

    public LocaleBundleDtos.ResolvedMessageResponse toResolvedMessageResponse(ResolvedLocaleMessageView view) {
        return new LocaleBundleDtos.ResolvedMessageResponse(
                view.bundleCode(),
                view.resourceKey(),
                view.requestedLocale(),
                view.resolvedLocale(),
                view.resourceValue(),
                view.tenantId(),
                view.usedFallback()
        );
    }

    private LocaleBundleDtos.EntryResponse toEntryResponse(LocaleResourceEntryView view) {
        return new LocaleBundleDtos.EntryResponse(
                view.id(),
                view.localeBundleId(),
                view.resourceKey(),
                view.resourceValue(),
                view.version(),
                view.active()
        );
    }
}
