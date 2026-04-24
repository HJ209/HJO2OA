package com.hjo2oa.portal.personalization.interfaces;

import com.hjo2oa.portal.personalization.domain.QuickAccessEntry;
import com.hjo2oa.portal.personalization.domain.QuickAccessEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuickAccessEntryRequest(
        @NotNull QuickAccessEntryType entryType,
        @NotBlank @Size(max = 128) String targetCode,
        @Size(max = 512) String targetLink,
        @Size(max = 128) String icon,
        Integer sortOrder,
        Boolean pinned
) {

    public QuickAccessEntry toDomain() {
        return new QuickAccessEntry(
                entryType,
                targetCode,
                targetLink,
                icon,
                sortOrder == null ? 0 : sortOrder,
                Boolean.TRUE.equals(pinned)
        );
    }
}
