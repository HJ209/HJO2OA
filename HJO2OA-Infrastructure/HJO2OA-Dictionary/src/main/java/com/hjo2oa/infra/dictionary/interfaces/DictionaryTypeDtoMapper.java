package com.hjo2oa.infra.dictionary.interfaces;

import com.hjo2oa.infra.dictionary.domain.DictionaryItemView;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeView;
import org.springframework.stereotype.Component;

@Component
public class DictionaryTypeDtoMapper {

    public DictionaryTypeDtos.DictionaryTypeResponse toResponse(DictionaryTypeView view) {
        return new DictionaryTypeDtos.DictionaryTypeResponse(
                view.id(),
                view.code(),
                view.name(),
                view.category(),
                view.hierarchical(),
                view.cacheable(),
                view.status(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt(),
                view.items().stream().map(this::toResponse).toList()
        );
    }

    public DictionaryTypeDtos.DictionaryItemResponse toResponse(DictionaryItemView view) {
        return new DictionaryTypeDtos.DictionaryItemResponse(
                view.id(),
                view.dictionaryTypeId(),
                view.itemCode(),
                view.displayName(),
                view.parentItemId(),
                view.sortOrder(),
                view.enabled(),
                view.multiLangValue()
        );
    }
}
