package com.hjo2oa.portal.aggregation.api.infrastructure;

import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.ContentSubjectContext;
import com.hjo2oa.content.search.application.ContentSearchApplicationService;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.PortalContentQuery;
import com.hjo2oa.portal.aggregation.api.domain.PortalContentCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalContentCardDataProvider;
import com.hjo2oa.portal.aggregation.api.domain.PortalContentItem;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCardDataProvider;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ContentSearchPortalContentCardDataProvider implements PortalContentCardDataProvider {

    private final PortalIdentityCardDataProvider identityCardDataProvider;
    private final ContentSearchApplicationService contentSearchApplicationService;

    public ContentSearchPortalContentCardDataProvider(
            PortalIdentityCardDataProvider identityCardDataProvider,
            ContentSearchApplicationService contentSearchApplicationService
    ) {
        this.identityCardDataProvider = identityCardDataProvider;
        this.contentSearchApplicationService = contentSearchApplicationService;
    }

    @Override
    public PortalContentCard currentContentCard() {
        PortalIdentityCard identity = identityCardDataProvider.currentIdentity();
        UUID tenantId = parseUuid(identity.tenantId());
        if (tenantId == null) {
            return PortalContentCard.empty();
        }
        var feed = contentSearchApplicationService.latestForPortal(new PortalContentQuery(
                tenantId,
                null,
                null,
                8,
                new ContentSubjectContext(
                        parseUuid(identity.personId()),
                        parseUuid(identity.assignmentId()),
                        parseUuid(identity.positionId()),
                        parseUuid(identity.departmentId()),
                        java.util.Set.of()
                )
        ));
        return new PortalContentCard(
                feed.total(),
                feed.latestArticles().stream()
                        .map(item -> new PortalContentItem(
                                toString(item.articleId()),
                                toString(item.publicationId()),
                                toString(item.categoryId()),
                                item.title(),
                                item.summary(),
                                item.authorName(),
                                item.tags(),
                                item.publishedAt(),
                                item.hotScore()
                        ))
                        .toList()
        );
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String toString(UUID value) {
        return value == null ? null : value.toString();
    }
}
