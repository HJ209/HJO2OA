package com.hjo2oa.infra.data.i18n.interfaces;

import com.hjo2oa.infra.data.i18n.domain.TranslationEntryView;
import com.hjo2oa.infra.data.i18n.domain.TranslationResolutionView;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TranslationEntryDtoMapper {

    public TranslationEntryDtos.EntryResponse toEntryResponse(TranslationEntryView view) {
        return new TranslationEntryDtos.EntryResponse(
                view.id(),
                view.entityType(),
                view.entityId(),
                view.fieldName(),
                view.locale(),
                view.translatedValue(),
                view.translationStatus(),
                view.tenantId(),
                view.updatedBy(),
                view.updatedAt()
        );
    }

    public List<TranslationEntryDtos.EntryResponse> toEntryResponses(List<TranslationEntryView> views) {
        return views.stream().map(this::toEntryResponse).toList();
    }

    public TranslationEntryDtos.ResolveResponse toResolveResponse(TranslationResolutionView view) {
        return new TranslationEntryDtos.ResolveResponse(
                view.entryId(),
                view.entityType(),
                view.entityId(),
                view.fieldName(),
                view.requestedLocale(),
                view.resolvedLocale(),
                view.resolvedValue(),
                view.translationStatus(),
                view.resolveSource(),
                view.fallbackApplied(),
                view.tenantId()
        );
    }
}
