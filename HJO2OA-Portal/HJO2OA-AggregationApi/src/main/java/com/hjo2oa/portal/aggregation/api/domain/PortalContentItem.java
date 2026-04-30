package com.hjo2oa.portal.aggregation.api.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PortalContentItem(
        String articleId,
        String publicationId,
        String categoryId,
        String title,
        String summary,
        String authorName,
        List<String> tags,
        Instant publishedAt,
        BigDecimal hotScore
) {

    public PortalContentItem {
        tags = tags == null ? List.of() : List.copyOf(tags);
        hotScore = hotScore == null ? BigDecimal.ZERO : hotScore;
    }
}
