package com.hjo2oa.portal.aggregation.api.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.content.permission.application.ContentPermissionApplicationService;
import com.hjo2oa.content.permission.infrastructure.InMemoryPublicationScopeRepository;
import com.hjo2oa.content.search.application.ContentSearchApplicationService;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.ContentIndexCommand;
import com.hjo2oa.content.search.infrastructure.InMemoryContentSearchIndexRepository;
import com.hjo2oa.portal.aggregation.api.domain.PortalContentCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCardDataProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentSearchPortalContentCardDataProviderTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PERSON_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID ARTICLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID PUBLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final Instant NOW = Instant.parse("2026-04-20T10:00:00Z");

    @Test
    void shouldLoadPublishedContentForPortalWidgetFromSearchIndex() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        ContentPermissionApplicationService permissionService = new ContentPermissionApplicationService(
                new InMemoryPublicationScopeRepository(),
                event -> {
                },
                clock
        );
        ContentSearchApplicationService searchService = new ContentSearchApplicationService(
                new InMemoryContentSearchIndexRepository(),
                permissionService,
                event -> {
                },
                clock
        );
        searchService.index(new ContentIndexCommand(
                TENANT_ID,
                ARTICLE_ID,
                PUBLICATION_ID,
                CATEGORY_ID,
                "门户公告",
                "摘要",
                "正文",
                PERSON_ID,
                "Editor",
                List.of("portal"),
                NOW,
                BigDecimal.TEN
        ));
        ContentSearchPortalContentCardDataProvider provider =
                new ContentSearchPortalContentCardDataProvider(identityProvider(), searchService);

        PortalContentCard card = provider.currentContentCard();

        assertThat(card.totalCount()).isEqualTo(1);
        assertThat(card.latestArticles()).hasSize(1);
        assertThat(card.latestArticles().get(0).articleId()).isEqualTo(ARTICLE_ID.toString());
        assertThat(card.latestArticles().get(0).title()).isEqualTo("门户公告");
    }

    private static PortalIdentityCardDataProvider identityProvider() {
        return () -> new PortalIdentityCard(
                TENANT_ID.toString(),
                PERSON_ID.toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "Position",
                "Org",
                "Dept",
                "PRIMARY",
                NOW
        );
    }
}
