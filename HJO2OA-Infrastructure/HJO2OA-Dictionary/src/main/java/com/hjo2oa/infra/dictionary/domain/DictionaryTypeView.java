package com.hjo2oa.infra.dictionary.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DictionaryTypeView(
        UUID id,
        String code,
        String name,
        String category,
        boolean hierarchical,
        boolean cacheable,
        int sortOrder,
        boolean systemManaged,
        DictionaryStatus status,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        List<DictionaryItemView> items
) {

    public DictionaryTypeView {
        items = List.copyOf(items);
    }
}
